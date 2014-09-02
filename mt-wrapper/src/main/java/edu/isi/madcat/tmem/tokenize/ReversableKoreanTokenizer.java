package edu.isi.madcat.tmem.tokenize;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import edu.isi.madcat.tmem.alignment.AlignmentGenerator;
import edu.isi.madcat.tmem.alignment.AlignmentPair;
import edu.isi.madcat.tmem.alignment.Range;
import edu.isi.madcat.tmem.alignment.TokenAlignment;
import edu.isi.madcat.tmem.alignment.WerAlignmentScorer;
import edu.isi.madcat.tmem.logging.ExceptionHandler;
import edu.isi.madcat.tmem.lookup.QueryHash;
import edu.isi.madcat.tmem.sql.InfileWriter;
import edu.isi.madcat.tmem.sql.SqlHandler;
import edu.isi.madcat.tmem.sql.SqlManager;
import edu.isi.madcat.tmem.utils.ParameterMap;
import edu.isi.madcat.tmem.utils.RegexReplacer;
import edu.isi.madcat.tmem.utils.TextSegment;
import edu.isi.madcat.tmem.utils.TextSegmentIterator;

public class ReversableKoreanTokenizer extends ReversableTokenizer {
  private static String TOTAL_COUNT_IDENTIFIER = "ZBMMKALPIJPFAFHJFA-TOTAL_COUNT";
  private static Set<Character> characterSet = null;

  static {
    characterSet = new HashSet<Character>();
    List<Range> ranges = new ArrayList<Range>();
    ranges.add(new Range(0xac00, 0xd7af));
    ranges.add(new Range(0x1100, 0x11ff));
    ranges.add(new Range(0x3130, 0x318f));
    ranges.add(new Range(0x3200, 0x32ff));
    ranges.add(new Range(0xa960, 0xa97f));
    ranges.add(new Range(0xd7b0, 0xd7ff));
    for (Range r : ranges) {
      int start = r.getStart();
      int end = r.getEnd();
      for (int i = start; i <= end; i++) {
        characterSet.add((char) i);
      }
    }
  }

  public SqlManager getSqlManager() {
    return sqlManager;
  }

  public void setSqlManager(SqlManager sqlManager) {
    this.sqlManager = sqlManager;
  }

  class LexiconEntry {
    private String rawWord;
    private List<String> tokens;
    private int count;

    public LexiconEntry(String rawWord, List<String> tokens, int count) {
      super();
      this.rawWord = rawWord;
      this.tokens = tokens;
      this.count = count;
    }

    public int getCount() {
      return count;
    }

    public String getRawWord() {
      return rawWord;
    }

    public List<String> getTokens() {
      return tokens;
    }

    public void setCount(int count) {
      this.count = count;
    }

    public void setRawWord(String rawWord) {
      this.rawWord = rawWord;
    }

    public void setTokens(List<String> tokens) {
      this.tokens = tokens;
    }

  }

  class TokenInstance {
    private int start;

    private int end;

    private int count;

    public TokenInstance(int start, int end, int count) {
      super();
      this.start = start;
      this.end = end;
      this.count = count;
    }

    public int getCount() {
      return count;
    }

    public int getEnd() {
      return end;
    }

    public int getStart() {
      return start;
    }

    public void setCount(int count) {
      this.count = count;
    }

    public void setEnd(int end) {
      this.end = end;
    }

    public void setStart(int start) {
      this.start = start;
    }
  }

  private RegexReplacer transformer;
  private RegexReplacer splitter;
  private boolean useLexicon;
  private boolean useSql;
  private boolean isVerbose;
  private Map<String, LexiconEntry> lexicon;
  private Map<String, Integer> tokenCounts;
  private int totalCount;
  private SqlManager sqlManager;

  public ReversableKoreanTokenizer() {
    // character replacement
    transformer = new RegexReplacer();
    transformer.addRegex("(\u201C|\u201D)", "\"");
    transformer.addRegex("(\u2018|2019)", "'");
    transformer.addRegex("(\\.\\.\\.+)", "...");
    transformer.addRegex("(\u00B7+)", "\u00B7");

    // tokenization
    splitter = new RegexReplacer();
    splitter.addRegex("([,;:@#$%&`])", " $1 ");
    splitter.addRegex("([!?])", " $1 ");
    splitter.addRegex("(/)", " $1 ");
    splitter.addRegex("(/\\s+/)", "//");
    splitter.addRegex("(['\"])", " $1 ");
    splitter.addRegex("([\\]\\[(){}<>])", " $1 ");
    splitter.addRegex("(\\.\\.\\.)", " $1 ");
    splitter.addRegex("(\\.)", " . ");
    splitter.addRegex("(--)", " $1 ");
    splitter.addRegex("(\u00B7)", " $1 ");
    splitter
        .addRegex(
            "([\u0021-\u007E])([\uAC00-\uD7AF\u1100-\u11FF\u3130-\u318F\u3200-\u32FF\uA960-\uA97F\uD7B0-\uD7FF])",
            "$1 $2"); // any ascii next to any korean
    splitter
        .addRegex(
            "([\uAC00-\uD7AF\u1100-\u11FF\u3130-\u318F\u3200-\u32FF\uA960-\uA97F\uD7B0-\uD7FF])([\u0021-\u007E])",
            "$1 $2"); // any ascii next to any korean
    useLexicon = false;
    isVerbose = false;
  }

  public void initialize(ParameterMap params) {
    if (params != null) {
      useLexicon = params.getBoolean("use_lexicon");
      useSql = params.getBoolean("use_sql");

      sqlManager = null;
      if (params.hasParam("sql_config_file")) {
        String sqlConfigFile = params.getString("sql_config_file");
        sqlManager = new SqlManager(sqlConfigFile);
      }
      totalCount = -1;
      if (useLexicon) {
        if (!useSql) {
          lexicon = new HashMap<String, LexiconEntry>();
          String lexiconFile = params.getStringRequired("lexicon_file");
          TextSegment segment = null;
          TextSegmentIterator segIt = new TextSegmentIterator(lexiconFile);
          while ((segment = segIt.next()) != null) {
            String rawWord = segment.getRequired("RAW_WORD");
            String[] tokenizedWordArray =
                StringUtils.split(segment.getRequired("TOKENIZED_WORD"), " ");
            int count = Integer.parseInt(segment.getRequired("COUNT"));
            List<String> tokens = Arrays.asList(tokenizedWordArray);
            LexiconEntry entry = new LexiconEntry(rawWord, tokens, count);
            lexicon.put(entry.getRawWord(), entry);
          }
          tokenCounts = new HashMap<String, Integer>();
          totalCount = 0;
          for (Map.Entry<String, LexiconEntry> entry : lexicon.entrySet()) {
            LexiconEntry lexEntry = entry.getValue();
            int entryCount = lexEntry.getCount();
            for (String token : lexEntry.getTokens()) {
              Integer countObject = tokenCounts.get(token);
              int count = 0;
              if (countObject != null) {
                count = countObject.intValue();
              }
              count += entryCount;
              tokenCounts.put(token, count);
              totalCount += count;
            }
          }
          segIt.close();
        } else {
          SqlHandler sqlHandler = sqlManager.createHandler();
          PreparedStatement statement =
              sqlHandler
                  .prepareStatement("SELECT token_key, count FROM kortok_token_counts WHERE token_key = ?");
          try {
            statement.setString(1, QueryHash.getHashString(TOTAL_COUNT_IDENTIFIER));
          } catch (SQLException e) {
            ExceptionHandler.handle(e);
          }
          List<List<String>> results = sqlHandler.executeQuery(statement);
          if (results.size() == 0) {
            throw new RuntimeException("Unable to get total count");
          }
          totalCount = Integer.parseInt(results.get(0).get(1));
          sqlHandler.close();
        }
      }
    }
    isVerbose = params.getBoolean("is_verbose");
  }

  public void createSqlLexicon(String workDir) {
    String lexiconFileName = workDir + "/kortok_lexicon.csv";
    InfileWriter lexiconWriter = new InfileWriter(lexiconFileName, 255);
    for (Map.Entry<String, LexiconEntry> e : lexicon.entrySet()) {
      LexiconEntry lexEntry = e.getValue();
      List<String> row = new ArrayList<String>();
      row.add(QueryHash.getHashString(lexEntry.getRawWord()));
      row.add(StringUtils.join(lexEntry.getTokens(), " "));
      lexiconWriter.write(row);
    }
    lexiconWriter.close();

    String tokenCountsFileName = workDir + "/kortok_token_counts.csv";
    InfileWriter tokenCountsWriter = new InfileWriter(tokenCountsFileName);
    for (Map.Entry<String, Integer> e : tokenCounts.entrySet()) {
      String token = e.getKey();
      Integer count = e.getValue();

      List<String> row = new ArrayList<String>();
      row.add(QueryHash.getHashString(token));
      row.add(count.toString());
      tokenCountsWriter.write(row);
    }
    List<String> row = new ArrayList<String>();
    row.add(QueryHash.getHashString(TOTAL_COUNT_IDENTIFIER));
    row.add("" + totalCount);
    tokenCountsWriter.write(row);
    tokenCountsWriter.close();

    // @formatter:off
    SqlHandler sqlHandler = sqlManager.createHandler();
    sqlHandler.executeStatement("DROP TABLE IF EXISTS kortok_lexicon");
    sqlHandler.executeStatement("DROP TABLE IF EXISTS kortok_token_counts");
    
    // table: kortok_lexicon
    sqlHandler.executeStatement("CREATE TABLE kortok_lexicon ("
        + "word_key CHAR("+QueryHash.STRING_LENGTH+"), "
        + "tokenized_word BLOB, "
        + "INDEX word_key_index (word_key("+QueryHash.STRING_LENGTH+")) "
        + ")");
    
    // table: kortok_token_counts
    sqlHandler.executeStatement("CREATE TABLE kortok_token_counts ("
        + "token_key CHAR("+QueryHash.STRING_LENGTH+"), "
        + "count INT, "
        + "INDEX token_key_index (token_key("+QueryHash.STRING_LENGTH+")) "
        + ")");
    sqlHandler.close();
    // @formatter:on

    InfileWriter.writeFile(sqlManager, lexiconFileName, "kortok_lexicon");

    InfileWriter.writeFile(sqlManager, tokenCountsFileName, "kortok_token_counts");
  }

  TokenAlignment getStringToWordAlignment(String rawWord, List<String> tokens) {
    List<String> rawChars = SingleCharacterTokenizer.tokenizeStatic(rawWord, null);
    WerAlignmentScorer scorer = new WerAlignmentScorer(rawChars, tokens);
    scorer.setMaxTokensPerChunk(20);
    TokenAlignment alignment = AlignmentGenerator.heuristicAlignment(scorer);
    if (alignment == null) {
      alignment = new TokenAlignment();
      for (int i = 0; i < tokens.size(); i++) {
        alignment.add(0, rawWord.length() - 1, i, i);
      }
    }
    return alignment;
  }

  @Override
  public List<String> tokenize(String input, TokenAlignment alignment) {
    return tokenize(input, alignment, null, null);
  }

  public List<String> tokenize(String input, TokenAlignment alignment,
      List<String> outputRawTokens, List<Integer> splitAlignment) {
    TokenAlignment rawTokenAlignment = new TokenAlignment();
    List<String> rawTokens = CharacterTokenizer.WHITESPACE.tokenize(input, rawTokenAlignment);
    List<String> outputTokens = new ArrayList<String>();
    List<TokenAlignment> wordAlignment = new ArrayList<TokenAlignment>();
    List<Integer> outputIndexes = new ArrayList<Integer>();
    if (outputRawTokens != null) {
      for (String token : rawTokens) {
        outputRawTokens.add(token);
      }
    }
    for (int i = 0; i < rawTokens.size(); i++) {
      String rawToken = rawTokens.get(i);
      List<String> splitTokens = getOutputTokens(rawToken);
      TokenAlignment curAlign = getStringToWordAlignment(rawToken, splitTokens);
      int startIndex = outputTokens.size();
      outputIndexes.add(startIndex);
      for (int j = 0; j < splitTokens.size(); j++) {
        outputTokens.add(splitTokens.get(j));
      }
      if (splitAlignment != null) {
        splitAlignment.add(startIndex);
      }
      wordAlignment.add(curAlign);
    }

    if (alignment != null) {
      Map<Range, Range> rawRangeMap = rawTokenAlignment.reverse().createRangeMap();
      for (int i = 0; i < rawTokens.size(); i++) {
        TokenAlignment curAlign = wordAlignment.get(i);
        Range rawRange = rawRangeMap.get(new Range(i, i));
        if (rawRange == null) {
          throw new RuntimeException("Cannot find raw input for token index: " + i);
        }
        int tokenOffset = outputIndexes.get(i);
        int rawStart = rawRange.getStart();
        for (AlignmentPair ap : curAlign) {
          int rawWordStart = ap.getInput().getStart();
          int rawWordEnd = ap.getInput().getEnd();
          int tokenStartIndex = ap.getOutput().getStart();
          int tokenEndIndex = ap.getOutput().getEnd();
          for (int j = tokenStartIndex; j <= tokenEndIndex; j++) {
            alignment.add(rawStart + rawWordStart, rawStart + rawWordEnd, tokenOffset + j,
                tokenOffset + j);
          }
        }
      }
      // for (AlignmentPair ap : alignment) {
      // int rawStart = ap.getInput().getStart();
      // int rawEnd = ap.getInput().getEnd();
      // int tokStart = ap.getOutput().getStart();
      // int tokEnd = ap.getOutput().getEnd();
      // if (tokStart != tokEnd) {
      // System.exit(1);
      // }
      // System.out.println("TOK " + outputTokens.get(tokStart) + " => "
      // + input.substring(rawStart, rawEnd + 1));
      // }
    }
    return outputTokens;
  }

  private List<String> lookupInLexicon(String word) {
    if (useSql) {
      SqlHandler sqlHandler = sqlManager.createHandler();
      PreparedStatement statement =
          sqlHandler
              .prepareStatement("SELECT tokenized_word FROM kortok_lexicon as k WHERE k.word_key = ?");
      try {
        statement.setString(1, QueryHash.getHashString(word));
      } catch (SQLException e) {
        ExceptionHandler.handle(e);
      }

      List<List<String>> results = sqlHandler.executeQuery(statement);
      sqlHandler.close();
      if (results.size() == 0) {
        return null;
      }
      String tokenizedString = results.get(0).get(0);
      List<String> tokens = Arrays.asList(StringUtils.split(tokenizedString, " "));
      if (isVerbose) {
        System.out.println(String.format("Seg lexicon: %s => %s", word, StringUtils.join(tokens,
            " ")));
      }
      return tokens;
    } else {
      LexiconEntry entry = lexicon.get(word);
      if (entry == null) {
        return null;
      }
      if (isVerbose) {
        System.out.println(String.format("Seg lexicon: %s => %s", word, StringUtils.join(entry
            .getTokens(), " ")));
      }
      return entry.getTokens();
    }
  }

  private class TokenPointer {
    public String getNgram() {
      return ngram;
    }

    public int getStart() {
      return start;
    }

    public int getEnd() {
      return end;
    }

    private String ngram;
    private int start;
    private int end;

    public TokenPointer(String ngram, int start, int end) {
      super();
      this.ngram = ngram;
      this.start = start;
      this.end = end;
    }
  }

  private List<String> segmentWord(String word) {
    List<String> tokens = new ArrayList<String>();
    if (!useLexicon) {
      tokens.add(word);
      return tokens;
    }
    List<String> lexiconTokens = lookupInLexicon(word);
    if (lexiconTokens != null) {
      return lexiconTokens;
    }
    int maxChars = 20;
    if (word.length() >= maxChars) {
      tokens.add(word);
      return tokens;
    }
    List<TokenPointer> ngrams = new ArrayList<TokenPointer>();

    for (int i = 0; i < word.length(); i++) {
      for (int j = i; j < word.length(); j++) {
        String ngram = word.substring(i, j + 1);
        TokenPointer tokenPointer = new TokenPointer(ngram, i, j + 1);
        ngrams.add(tokenPointer);
      }
    }
    List<TokenInstance> instances = getTokenInstances(ngrams);

    int maxHyps = 100;
    List<List<List<Integer>>> partialHyps = new ArrayList<List<List<Integer>>>();
    for (int i = 0; i < word.length() + 1; i++) {
      partialHyps.add(new ArrayList<List<Integer>>());
    }
    partialHyps.get(0).add(new ArrayList<Integer>());
    List<List<Integer>> instanceToEnd = new ArrayList<List<Integer>>();
    for (int i = 0; i < word.length() + 1; i++) {
      instanceToEnd.add(new ArrayList<Integer>());
    }
    for (int i = 0; i < instances.size(); i++) {
      TokenInstance instance = instances.get(i);
      instanceToEnd.get(instance.getEnd()).add(i);
    }
    for (int i = 0; i < word.length() + 1; i++) {
      List<Integer> instanceIndexes = instanceToEnd.get(i);
      for (int j = 0; j < instanceIndexes.size(); j++) {
        TokenInstance instance = instances.get(instanceIndexes.get(j));
        List<List<Integer>> startHyps = partialHyps.get(instance.getStart());
        List<List<Integer>> endHyps = partialHyps.get(instance.getEnd());
        for (int k = 0; k < startHyps.size(); k++) {
          List<Integer> hyp = new ArrayList<Integer>(startHyps.get(k));
          hyp.add(instanceIndexes.get(j));
          if (endHyps.size() < maxHyps) {
            endHyps.add(hyp);
          }
        }
      }
    }
    List<List<Integer>> hyps = partialHyps.get(word.length());
    if (hyps.size() == 0) {
      tokens.add(word);
      return tokens;
    }
    int bestHypIndex = 0;
    double bestProb = 0.0;
    for (int i = 0; i < hyps.size(); i++) {
      double totalLogProb = 0.0;
      for (int j = 0; j < hyps.get(i).size(); j++) {
        TokenInstance instance = instances.get(hyps.get(i).get(j));
        double logProb = Math.log((double) instance.getCount() / (double) totalCount + 1e-16);
        totalLogProb += logProb;
      }
      if (i == 0 || totalLogProb > bestProb) {
        bestHypIndex = i;
        bestProb = totalLogProb;
      }
    }
    List<Integer> bestHyp = hyps.get(bestHypIndex);
    for (int i = 0; i < bestHyp.size(); i++) {
      TokenInstance instance = instances.get(bestHyp.get(i));
      String ngram = word.substring(instance.getStart(), instance.getEnd());
      tokens.add(ngram);
    }
    if (isVerbose) {
      System.out.println(String.format("Segmented novel word: %s => %s (prob = %.3f)", word,
          StringUtils.join(tokens, " "), bestProb));
    }
    return tokens;
  }

  private List<TokenInstance> getTokenInstances(List<TokenPointer> tokenPointers) {
    List<TokenInstance> instances = new ArrayList<TokenInstance>();
    if (useSql) {
      int maxPerQuery = 30;
      int numQueries = tokenPointers.size() / maxPerQuery;
      if (tokenPointers.size() % maxPerQuery != 0) {
        numQueries++;
      }
      // Since we hash everything, SQL injections can't be used here
      int k = 0;
      for (int i = 0; i < numQueries; i++) {
        StringBuilder sb = new StringBuilder();
        Map<String, TokenPointer> ptrMap = new HashMap<String, TokenPointer>();
        for (int j = 0; j < maxPerQuery && k < tokenPointers.size(); j++, k++) {
          if (j > 0) {
            sb.append(",");
          }
          String hashValue = QueryHash.getHashString(tokenPointers.get(k).getNgram());
          sb.append("'" + hashValue + "'");
          ptrMap.put(hashValue, tokenPointers.get(k));
        }
        SqlHandler sqlHandler = sqlManager.createHandler();
        PreparedStatement statement =
            sqlHandler
                .prepareStatement("SELECT token_key, count FROM kortok_token_counts WHERE token_key IN ("
                    + sb.toString() + ")");
        List<List<String>> results = sqlHandler.executeQuery(statement);
        for (int j = 0; j < results.size(); j++) {
          String key = results.get(j).get(0);
          int count = Integer.parseInt(results.get(j).get(1));
          TokenPointer tokenPointer = ptrMap.get(key);
          if (tokenPointer != null) {
            instances.add(new TokenInstance(tokenPointer.getStart(), tokenPointer.getEnd(), count));
          }
        }
        sqlHandler.close();
      }
    } else {
      for (TokenPointer tp : tokenPointers) {
        Integer count = tokenCounts.get(tp.getNgram());
        if (count != null) {
          TokenInstance instance = new TokenInstance(tp.getStart(), tp.getEnd(), count);
          instances.add(instance);
        }
      }
    }
    return instances;
  }

  private List<String> getOutputTokens(String rawToken) {
    String processedToken = rawToken;
    processedToken = transformer.process(processedToken);
    processedToken = StringUtils.trim(processedToken);
    String[] tokenArray = processedToken.split("\\s+");
    List<String> tokens = new ArrayList<String>();
    for (String token : tokenArray) {
      String splitToken = StringUtils.trim(splitter.process(token));
      String[] splitTokenArray = splitToken.split("\\s+");
      for (String subToken : splitTokenArray) {
        List<String> splitWords = segmentWord(subToken);
        for (String splitWord : splitWords) {
          tokens.add(splitWord);
        }
      }
    }
    return tokens;
  }

  public boolean isLanguageChar(char c) {
    if (characterSet.contains(new Character(c))) {
      return true;
    }
    return false;
  }
}
