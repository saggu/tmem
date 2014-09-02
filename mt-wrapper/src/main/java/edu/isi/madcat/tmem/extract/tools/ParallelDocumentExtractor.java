package edu.isi.madcat.tmem.extract.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import edu.isi.madcat.tmem.alignment.AlignmentPair;
import edu.isi.madcat.tmem.alignment.Range;
import edu.isi.madcat.tmem.alignment.TokenAlignment;
import edu.isi.madcat.tmem.logging.ExceptionHandler;
import edu.isi.madcat.tmem.tokenize.Tokenizer;
import edu.isi.madcat.tmem.tokenize.TokenizerFactory;
import edu.isi.madcat.tmem.training.tools.SentenceSegmentor;
import edu.isi.madcat.tmem.utils.ParameterMap;
import edu.isi.madcat.tmem.utils.TextSegment;
import edu.isi.madcat.tmem.utils.TextSegmentIterator;
import edu.isi.madcat.tmem.utils.Utils;

public class ParallelDocumentExtractor {
  private enum LineLabel {
    SOURCE, TARGET, BLANK
  }

  class SpanRange {
    private int sourceStart;

    private int sourceEnd;

    private int targetStart;

    private int targetEnd;

    public SpanRange(int sourceStart, int sourceEnd, int targetStart, int targetEnd) {
      super();
      this.sourceStart = sourceStart;
      this.sourceEnd = sourceEnd;
      this.targetStart = targetStart;
      this.targetEnd = targetEnd;
    }

    public int getSourceEnd() {
      return sourceEnd;
    }

    public int getSourceStart() {
      return sourceStart;
    }

    public int getTargetEnd() {
      return targetEnd;
    }

    public int getTargetStart() {
      return targetStart;
    }

    public void setSourceEnd(int sourceEnd) {
      this.sourceEnd = sourceEnd;
    }

    public void setSourceStart(int sourceStart) {
      this.sourceStart = sourceStart;
    }

    public void setTargetEnd(int targetEnd) {
      this.targetEnd = targetEnd;
    }

    public void setTargetStart(int targetStart) {
      this.targetStart = targetStart;
    }

    @Override
    public String toString() {
      return "SpanRange [sourceStart=" + sourceStart + ", sourceEnd=" + sourceEnd
          + ", targetStart=" + targetStart + ", targetEnd=" + targetEnd + "]";
    }
  }

  class LabelSpan {
    private int start;

    private int end;

    private LineLabel label;

    public LabelSpan(int start, int end, LineLabel label) {
      super();
      this.start = start;
      this.end = end;
      this.label = label;
    }

    public int getEnd() {
      return end;
    }

    public LineLabel getLabel() {
      return label;
    }

    public int getStart() {
      return start;
    }

    public void setEnd(int end) {
      this.end = end;
    }

    public void setLabel(LineLabel label) {
      this.label = label;
    }

    public void setStart(int start) {
      this.start = start;
    }
  }

  public class TextSpan {
    private int lineIndex;

    private int chunkIndex;

    private String rawText;

    private List<String> tokText;

    private TokenAlignment alignment;

    public TextSpan(int lineIndex, int chunkIndex, String rawText, List<String> tokText,
        TokenAlignment alignment) {
      super();
      this.lineIndex = lineIndex;
      this.chunkIndex = chunkIndex;
      this.rawText = rawText;
      this.tokText = tokText;
      this.alignment = alignment;
    }

    public TokenAlignment getAlignment() {
      return alignment;
    }

    public int getChunkIndex() {
      return chunkIndex;
    }

    public int getLineIndex() {
      return lineIndex;
    }

    public String getRawText() {
      return rawText;
    }

    public List<String> getTokText() {
      return tokText;
    }

    @Override
    public String toString() {
      return "TextSpan [lineIndex=" + lineIndex + ", chunkIndex=" + chunkIndex + ", rawText="
          + rawText + ", tokText=" + tokText + ", alignment=" + alignment + "]";
    }
  }

  class LexCounts {
    public Map<String, List<String>> getFwTrans() {
      return fwTrans;
    }

    public Map<String, List<String>> getBwTrans() {
      return bwTrans;
    }

    private Map<String, Double> jointCounts;
    private Map<String, Double> sourceCounts;
    private Map<String, Double> targetCounts;
    private Map<String, List<String>> fwTrans;
    private Map<String, List<String>> bwTrans;

    public LexCounts(String inputFile) {
      jointCounts = new HashMap<String, Double>();
      sourceCounts = new HashMap<String, Double>();
      targetCounts = new HashMap<String, Double>();
      fwTrans = new HashMap<String, List<String>>();
      bwTrans = new HashMap<String, List<String>>();

      Pattern p = Pattern.compile("^(.*) (.*) (.*)$");
      BufferedReader reader = null;
      try {
        reader = new BufferedReader(new InputStreamReader(Utils.openFile(inputFile)));
        String line = null;
        while ((line = reader.readLine()) != null) {
          Matcher m = p.matcher(line);
          if (!m.find()) {
            reader.close();
            throw new RuntimeException("Malformed line: " + line);
          }
          String source = m.group(1);
          String target = m.group(2);
          double count = Double.parseDouble(m.group(3));
          String joint = source + " " + target;
          addToTrans(source, target, fwTrans);
          addToTrans(target, source, bwTrans);
          addToMap(joint, count, jointCounts);
          addToMap(source, count, sourceCounts);
          addToMap(target, count, targetCounts);
        }
      } catch (FileNotFoundException e) {
        ExceptionHandler.handle(e);
      } catch (IOException e) {
        ExceptionHandler.handle(e);
      } finally {
        IOUtils.closeQuietly(reader);
      }
    }

    private void addToTrans(String source, String target, Map<String, List<String>> mapping) {
      List<String> targets = mapping.get(source);
      if (targets == null) {
        targets = new ArrayList<String>();
        mapping.put(source, targets);
      }
      targets.add(target);
    }

    private void addToMap(String key, double count, Map<String, Double> counts) {
      Double d = counts.get(key);
      if (d == null) {
        counts.put(key, count);
      } else {
        counts.put(key, d.doubleValue() + count);
      }
    }

    private double getJointCount(String source, String target) {
      Double d = jointCounts.get(source + " " + target);
      if (d == null) {
        return 0.0;
      }
      return d;
    }

    private double getSourceCount(String source) {
      Double d = sourceCounts.get(source);
      if (d == null) {
        return 0.0;
      }
      return d;
    }

    private double getTargetCount(String target) {
      Double d = targetCounts.get(target);
      if (d == null) {
        return 0.0;
      }
      return d;
    }

    double getBwProb(String source, String target) {
      double jointCount = getJointCount(source, target);
      double targetCount = getTargetCount(target);
      double prob = 0.0;
      if (targetCount > 0.0) {
        prob = jointCount / targetCount;
      }
      return prob;
    }

    double getFwProb(String source, String target) {
      double jointCount = getJointCount(source, target);
      double sourceCount = getSourceCount(source);
      double prob = 0.0;
      if (sourceCount > 0.0) {
        prob = jointCount / sourceCount;
      }
      return prob;
    }
  }

  public static void main(String[] args) {
    if (args.length != 1) {
      throw new RuntimeException("Usage: [app] parameter_file");
    }

    String parameterFile = args[0];
    ParameterMap params = new ParameterMap(parameterFile);
    ParallelDocumentExtractor processor = new ParallelDocumentExtractor(params);
    processor.process();
  }

  private String inputSpecifierFile;
  private Tokenizer sourceTokenizer;
  private Tokenizer targetTokenizer;
  private String outputFile;
  private LexCounts lexCounts;
  private double maxTokenRatio;
  private double tokenSmoothing;
  private double minMatchProb;
  private double minMatchCount;
  private double minMatchToKeep;
  private double sourceToTargetRatioMultiplier;
  private Set<String> functionWords;
  private SentenceSegmentor sentenceSegmentor;
  private Pattern newGroupPattern;
  private double minSourcePercent;
  private double minTargetPercent;
  private int maxWordsPerSegment;
  
  public ParallelDocumentExtractor(ParameterMap params) {
    inputSpecifierFile = params.getStringRequired("input_specifier_file");
    String sourceTokenizerName = params.getStringRequired("source_tokenizer_name");
    String sourceTokenizerParamFile = params.getStringRequired("source_tokenizer_param_file");
    String targetTokenizerName = params.getStringRequired("target_tokenizer_name");
    String targetTokenizerParamFile = params.getStringRequired("target_tokenizer_param_file");
    String functionWordFile = params.getString("function_word_file");
    String lexCountsFile = params.getStringRequired("lex_counts_file");
    String splitTokenFile = params.getStringRequired("split_token_file");
    outputFile = params.getString("output_file");
    sourceTokenizer = TokenizerFactory.create(sourceTokenizerName, sourceTokenizerParamFile);
    targetTokenizer = TokenizerFactory.create(targetTokenizerName, targetTokenizerParamFile);
    maxTokenRatio = 1.4;
    minSourcePercent = 0.6;
    minTargetPercent = 0.3;
    tokenSmoothing = 10.0;
    minMatchToKeep = 0.3;
    maxWordsPerSegment = 50;
    newGroupPattern = Pattern.compile("^[0-9]");
    sourceToTargetRatioMultiplier = 1.25;
    sourceTokenizer = TokenizerFactory.create(sourceTokenizerName, sourceTokenizerParamFile);
    targetTokenizer = TokenizerFactory.create(targetTokenizerName, targetTokenizerParamFile);
    lexCounts = null;
    if (lexCountsFile != null) {
      lexCounts = new LexCounts(lexCountsFile);
    }
    functionWords = null;
    if (functionWordFile != null && functionWordFile.length() > 0) {
      functionWords = new HashSet<String>();
      List<String> lines = null;
      try {
        lines = FileUtils.readLines(new File(functionWordFile));
      } catch (IOException e) {
        ExceptionHandler.handle(e);
      }
      for (String line : lines) {
        functionWords.add(line);
      }
    }

    minMatchProb = 0.005;
    minMatchCount = 1.0;
    int minBreakOver = 30;
    int mustBreakOver = 60;

    sentenceSegmentor = new SentenceSegmentor(splitTokenFile, minBreakOver, mustBreakOver);
  }

  public class SpanScoreComparator implements Comparator<Pair<TextSpan, Double>> {
    public int compare(Pair<TextSpan, Double> p1, Pair<TextSpan, Double> p2) {
      if (p2.getRight() < p1.getRight()) {
        return -1;
      } else if (p2.getRight() > p1.getRight()) {
        return 1;
      }
      return 0;
    }
  }

  public class Backpointer {
    @Override
    public String toString() {
      return "Backpointer [sourceStart=" + sourceStart + ", sourceEnd=" + sourceEnd
          + ", targetStart=" + targetStart + ", targetEnd=" + targetEnd + ", errorScore="
          + errorScore + ", sourceNullScore=" + sourceNullScore + ", targetNullScore="
          + targetNullScore + ", totalScore=" + totalScore + "]";
    }

    public double getTotalScore() {
      return totalScore;
    }

    public void setTotalScore(double totalScore) {
      this.totalScore = totalScore;
    }

    public int getSourceStart() {
      return sourceStart;
    }

    public void setSourceStart(int sourceStart) {
      this.sourceStart = sourceStart;
    }

    public int getSourceEnd() {
      return sourceEnd;
    }

    public void setSourceEnd(int sourceEnd) {
      this.sourceEnd = sourceEnd;
    }

    public int getTargetStart() {
      return targetStart;
    }

    public void setTargetStart(int targetStart) {
      this.targetStart = targetStart;
    }

    public int getTargetEnd() {
      return targetEnd;
    }

    public void setTargetEnd(int targetEnd) {
      this.targetEnd = targetEnd;
    }

    public double getErrorScore() {
      return errorScore;
    }

    public void setErrorScore(double errorScore) {
      this.errorScore = errorScore;
    }

    public double getSourceNullScore() {
      return sourceNullScore;
    }

    public void setSourceNullScore(double sourceNullScore) {
      this.sourceNullScore = sourceNullScore;
    }

    public double getTargetNullScore() {
      return targetNullScore;
    }

    public void setTargetNullScore(double targetNullScore) {
      this.targetNullScore = targetNullScore;
    }

    public Backpointer getPrevBp() {
      return prevBp;
    }

    public void setPrevBp(Backpointer prevBp) {
      this.prevBp = prevBp;
    }

    private int sourceStart;
    private int sourceEnd;
    private int targetStart;
    private int targetEnd;
    private double errorScore;
    private double sourceNullScore;
    private double targetNullScore;
    private Backpointer prevBp;
    private double totalScore;

    public Backpointer(int sourceStart, int sourceEnd, int targetStart, int targetEnd,
        double errorScore, double sourceNullScore, double targetNullScore, Backpointer prevBp) {
      super();
      this.sourceStart = sourceStart;
      this.sourceEnd = sourceEnd;
      this.targetStart = targetStart;
      this.targetEnd = targetEnd;
      this.errorScore = errorScore;
      this.sourceNullScore = sourceNullScore;
      this.targetNullScore = targetNullScore;
      this.prevBp = prevBp;
      this.totalScore = errorScore + sourceNullScore + targetNullScore;
      if (prevBp != null) {
        this.totalScore += prevBp.getTotalScore();
      }
    }
  }

  public class TranslationSet {
    @Override
    public String toString() {
      return "TranslationSet [targetWords=" + Arrays.toString(targetWords) + ", matchIds="
          + Arrays.toString(matchIds) + ", doesMatch=" + Arrays.toString(doesMatch)
          + ", wordCount=" + wordCount + "]";
    }

    public String[] getTargetWords() {
      return targetWords;
    }

    public void setTargetWords(String[] targetWords) {
      this.targetWords = targetWords;
    }

    public int[][] getMatchIds() {
      return matchIds;
    }

    public void setMatchIds(int[][] matchIds) {
      this.matchIds = matchIds;
    }

    public boolean[] getDoesMatch() {
      return doesMatch;
    }

    public void setDoesMatch(boolean[] doesMatch) {
      this.doesMatch = doesMatch;
    }

    public double getWordCount() {
      return wordCount;
    }

    public void setWordCount(double wordCount) {
      this.wordCount = wordCount;
    }

    String[] targetWords;
    int[][] matchIds;
    boolean[] doesMatch;
    double wordCount;

    public TranslationSet(String[] targetWords, int[][] matchIds, boolean[] doesMatch,
        double wordCount) {
      super();
      this.targetWords = targetWords;
      this.matchIds = matchIds;
      this.doesMatch = doesMatch;
      this.wordCount = wordCount;
    }
  }

  public void process() {
    TextSegmentIterator segIt = new TextSegmentIterator(inputSpecifierFile);
    Map<String, Map<String, String>> fileMap = new TreeMap<String, Map<String, String>>();
    TextSegment segment = null;
    while ((segment = segIt.next()) != null) {
      String setId = segment.getRequired("set_id");
      String fileIndex = segment.getRequired("file_index");
      String textFile = segment.getRequired("text_file");
      Map<String, String> mapping = fileMap.get(setId);
      if (mapping == null) {
        mapping = new TreeMap<String, String>();
        fileMap.put(setId, mapping);
      }
      mapping.put(fileIndex, textFile);
    }
    segIt.close();

    Writer outFp = Utils.createWriter(outputFile);
    for (Map.Entry<String, Map<String, String>> e1 : fileMap.entrySet()) {
      String docKey = e1.getKey();
      int segmentIndex = 1;
      Map<String, String> mapping = e1.getValue();
      if (mapping.size() == 1) {
        String sourceFile = mapping.get("0");
        List<String> lines = null;
        try {
          lines = FileUtils.readLines(new File(sourceFile));
        } catch (IOException e) {
          ExceptionHandler.handle(e);
        }
        List<TextSegment> segments = extractSegments(docKey, lines);
        for (TextSegment s : segments) {
          s.write(outFp);
        }
      } else if (mapping.size() == 2) {
        String sourceFile = mapping.get("0");
        String targetFile = mapping.get("1");
        System.out.println("*** Processing: " + sourceFile);
        List<String> sourceLines = null;
        List<String> targetLines = null;
        try {
          sourceLines = IOUtils.readLines(Utils.openFile(sourceFile), "utf-8");
          targetLines = IOUtils.readLines(Utils.openFile(targetFile), "utf-8");
        } catch (IOException e) {
          ExceptionHandler.handle(e);
        }
        System.out.println("Extracting Text");
        List<TextSpan> allSourceSpans = getSpans(sourceLines, sourceTokenizer);
        List<TextSpan> allTargetSpans = getSpans(targetLines, targetTokenizer);

        for (int i = 0; i < allSourceSpans.size(); i++) {
          System.out.println("Source [" + i + "]: "
              + StringUtils.join(allSourceSpans.get(i).getTokText(), " "));
        }
        for (int i = 0; i < allTargetSpans.size(); i++) {
          System.out.println("Target [" + i + "]: "
              + StringUtils.join(allTargetSpans.get(i).getTokText(), " "));
        }
        System.out.println("Aligning Text");

        System.out.println("Num Source Spans: " + allSourceSpans.size());
        System.out.println("Num Target Spans: " + allTargetSpans.size());
        int targetBuffer = 400;
        int maxJointSpans = 4;

        Backpointer[][] simMatrix =
            new Backpointer[allSourceSpans.size() + 1][allTargetSpans.size() + 1];

        simMatrix[0][0] = new Backpointer(-1, -1, -1, -1, 0.0, 0.0, 0.0, null);

        for (int i = 0; i < allSourceSpans.size(); i++) {
          double nullScore = getNullScore(allSourceSpans, i);
          simMatrix[i + 1][0] = new Backpointer(0, i, -1, -1, 0.0, nullScore, 0.0, simMatrix[i][0]);
        }

        for (int i = 0; i < allTargetSpans.size(); i++) {
          double nullScore = getNullScore(allTargetSpans, i);
          simMatrix[0][i + 1] = new Backpointer(-1, -1, 0, i, 0.0, 0.0, nullScore, simMatrix[0][i]);
        }

        TranslationSet[][] sourceTransSets = createTransSets(allSourceSpans, maxJointSpans, true);
        TranslationSet[][] targetTransSets = createTransSets(allTargetSpans, maxJointSpans, false);
        String[][][] sourceWords = getWords(allSourceSpans, maxJointSpans);
        String[][][] targetWords = getWords(allTargetSpans, maxJointSpans);

        for (int sourceEnd = 0; sourceEnd < allSourceSpans.size(); sourceEnd++) {
          System.out.println("Processing Source Span: " + sourceEnd);
          int targetIndex =
              (int) (((double) sourceEnd / (double) allSourceSpans.size()) * (double) allTargetSpans
                  .size());
          int firstTarget = Math.max(0, targetIndex - targetBuffer);
          int lastTarget = Math.min(allTargetSpans.size() - 1, targetIndex + targetBuffer);
          for (int targetEnd = 0; targetEnd < allTargetSpans.size(); targetEnd++) {
            // System.out.println("Source End: " + sourceEnd);
            // System.out.println("Target End: " + targetEnd);
            int sourceStart = Math.max(0, sourceEnd - maxJointSpans + 1);
            int targetStart = Math.max(0, targetEnd - maxJointSpans + 1);
            // source-target match
            for (int i = sourceStart; i <= sourceEnd; i++) {
              for (int j = targetStart; j <= targetEnd; j++) {
                int sourceLength = sourceEnd - i + 1;
                int targetLength = targetEnd - j + 1;
                int numSourceWords = getNumWords(allSourceSpans, i, sourceEnd);
                int numTargetWords = getNumWords(allTargetSpans, j, targetEnd);
                
                double manyToManyPenalty = 1.0;
                if (sourceLength > 1 && targetLength > 1) {
                  manyToManyPenalty = 1.1;
                  if (numSourceWords >= maxWordsPerSegment && numTargetWords >= maxWordsPerSegment) {
                    manyToManyPenalty = 2.0;
                  }
                }
                double errorScore = 1e30;
                if (j >= firstTarget && targetEnd <= lastTarget) {
                  if (passPreFilter(allSourceSpans, i, sourceEnd, allTargetSpans, j, targetEnd)) {
                    TranslationSet st = sourceTransSets[sourceEnd][sourceEnd - i];
                    String[] tw = targetWords[targetEnd][targetEnd - j];
                    double sourceErrorCount = computeErrorCount(st, tw);
                    TranslationSet tt = targetTransSets[targetEnd][targetEnd - j];
                    String[] sw = sourceWords[sourceEnd][sourceEnd - i];
                    double targetErrorCount = computeErrorCount(tt, sw);
                    errorScore = manyToManyPenalty * (sourceErrorCount + targetErrorCount);
                    // List<String> sourceWords2 = getTokWords(allSourceSpans, i, sourceEnd);
                    // List<String> targetWords2 = getTokWords(allTargetSpans, j, targetEnd);
                    // double errorScore2 = getErrorScore(sourceWords2, targetWords2);
                    // System.out.println(errorScore + " " + errorScore2);
                  }
                }
                Backpointer prevBp = simMatrix[i][j];
                Backpointer bp =
                    new Backpointer(i, sourceEnd, j, targetEnd, errorScore, 0.0, 0.0, prevBp);
                Backpointer currentBp = simMatrix[sourceEnd + 1][targetEnd + 1];
                if (currentBp == null || bp.getTotalScore() < currentBp.getTotalScore()) {
                  simMatrix[sourceEnd + 1][targetEnd + 1] = bp;
                }
              }
            }

            // null aligned source
            {
              double nullScore = getNullScore(allSourceSpans, sourceEnd);
              Backpointer prevBp = simMatrix[sourceEnd][targetEnd + 1];
              Backpointer bp =
                  new Backpointer(sourceEnd, sourceEnd, -1, -1, 0.0, nullScore, 0.0, prevBp);
              Backpointer currentBp = simMatrix[sourceEnd + 1][targetEnd + 1];
              if (currentBp == null || bp.getTotalScore() < currentBp.getTotalScore()) {
                simMatrix[sourceEnd + 1][targetEnd + 1] = bp;
              }
            }

            // null aligned target
            {
              double nullScore = getNullScore(allTargetSpans, targetEnd);
              Backpointer prevBp = simMatrix[sourceEnd + 1][targetEnd];
              Backpointer bp =
                  new Backpointer(-1, -1, targetEnd, targetEnd, 0.0, 0.0, nullScore, prevBp);
              Backpointer currentBp = simMatrix[sourceEnd + 1][targetEnd + 1];
              if (currentBp == null || bp.getTotalScore() < currentBp.getTotalScore()) {
                simMatrix[sourceEnd + 1][targetEnd + 1] = bp;
              }
            }
            // System.out.println(simMatrix[sourceEnd + 1][targetEnd + 1].toString());
          }
        }

        Backpointer currentBp = simMatrix[allSourceSpans.size()][allTargetSpans.size()];
        List<Backpointer> outputsBps = new ArrayList<Backpointer>();
        while (currentBp != null) {
          outputsBps.add(currentBp);
          currentBp = currentBp.getPrevBp();
        }
        Collections.reverse(outputsBps);
        for (int i = 0; i < outputsBps.size(); i++) {
          Backpointer bp = outputsBps.get(i);
          if (bp.getSourceStart() != -1 && bp.getTargetStart() != -1) {
            boolean isSourceLangauge =
                isLanguageSegment(allSourceSpans, bp.getSourceStart(), bp.getSourceEnd(),
                    sourceTokenizer);
            boolean isTargetLangauge =
                isLanguageSegment(allTargetSpans, bp.getTargetStart(), bp.getTargetEnd(),
                    targetTokenizer);
            System.out.println(String.format("*** Span: <%d, %d> => <%d, %d>", bp.getSourceStart(),
                bp.getSourceEnd(), bp.getTargetStart(), bp.getTargetEnd()));
            String rawSource = getRawText(allSourceSpans, bp.getSourceStart(), bp.getSourceEnd());
            String rawTarget = getRawText(allTargetSpans, bp.getTargetStart(), bp.getTargetEnd());
            String tokenizedSource =
                getTokenizedText(allSourceSpans, bp.getSourceStart(), bp.getSourceEnd());
            String tokenizedTarget =
                getTokenizedText(allTargetSpans, bp.getTargetStart(), bp.getTargetEnd());
            System.out.println("Source: " + rawSource);
            System.out.println("Target: " + rawTarget);
            if (isSourceLangauge && isTargetLangauge) {
              TextSegment s = new TextSegment();
              s.insert("GUID", String.format("[ExtractedSegment][%s][%05d]", docKey, segmentIndex));
              s.insert("RAW_SOURCE", rawSource);
              s.insert("RAW_TARGET", rawTarget);
              s.insert("TOKENIZED_SOURCE", tokenizedSource);
              s.insert("TOKENIZED_TARGET", tokenizedTarget);
              s.insert("ERROR_SCORE", "" + bp.getErrorScore());
              s.write(outFp);
              segmentIndex++;
            } else {
              System.out.println("SKIPPED");
            }
          } else if (bp.getSourceStart() == -1) {
            System.out.println(String.format("*** Null Target: <%d, %d>", bp.getTargetStart(), bp
                .getTargetEnd()));
          } else if (bp.getTargetStart() == -1) {
            System.out.println(String.format("*** Null Source: <%d, %d>", bp.getSourceStart(), bp
                .getSourceEnd()));
          }
        }
      }
    }
    IOUtils.closeQuietly(outFp);
  }

  private int getNumWords(List<TextSpan> spans, int start, int end) {
    int count = 0;
    for (int i = start; i <= end; i++ ) {
      count += spans.get(i).getTokText().size();
    }
    return count;
  }

  private boolean isLanguageSegment(List<TextSpan> spans, int start, int end, Tokenizer tokenizer) {
    double numLanguageChars = 0.0;
    double numTotalChars = 0.0;

    for (int i = start; i <= end; i++) {
      List<String> tokens = spans.get(i).getTokText();
      for (int j = 0; j < tokens.size(); j++) {
        String token = tokens.get(j);
        for (int k = 0; k < token.length(); k++) {
          if (tokenizer.isLanguageChar(token.charAt(k))) {
            numLanguageChars += 1.0;
          }
          numTotalChars += 1.0;
        }
      }
    }
    double minLanguageCharRatio = 0.3;
    double languageCharRatio = numLanguageChars / numTotalChars;
    if (languageCharRatio > minLanguageCharRatio) {
      return true;
    }
    return false;
  }

  private String[][][] getWords(List<TextSpan> spans, int maxJointSpans) {
    String[][][] allWords = new String[spans.size()][maxJointSpans][];
    for (int end = 0; end < spans.size(); end++) {
      int start = Math.max(0, end - maxJointSpans + 1);
      for (int i = start; i <= end; i++) {
        SortedSet<String> words = new TreeSet<String>();
        for (int j = i; j <= end; j++) {
          for (int k = 0; k < spans.get(j).getTokText().size(); k++) {
            words.add(processForMatch(spans.get(j).getTokText().get(k)));
          }
        }
        String[] wordArray = new String[words.size()];
        int index = 0;
        for (String word : words) {
          wordArray[index] = word;
          index++;
        }
        allWords[end][end - i] = wordArray;
      }
    }
    return allWords;
  }

  private String processForMatch(String str) {
    return str.toLowerCase();
  }

  private TranslationSet[][] createTransSets(List<TextSpan> spans, int maxJointSpans, boolean isFw) {
    Map<String, List<String>> lexTrans = null;
    if (isFw) {
      lexTrans = lexCounts.getFwTrans();
    } else {
      lexTrans = lexCounts.getBwTrans();
    }

    TranslationSet[][] allTransSets = new TranslationSet[spans.size()][maxJointSpans];
    for (int end = 0; end < spans.size(); end++) {
      int start = Math.max(0, end - maxJointSpans + 1);
      for (int i = start; i <= end; i++) {
        double wordCount = 0.0;
        List<String> sourceWords = new ArrayList<String>();
        for (int j = i; j <= end; j++) {
          wordCount += (double) spans.get(j).getTokText().size();

          for (int k = 0; k < spans.get(j).getTokText().size(); k++) {
            String sourceWord = spans.get(j).getTokText().get(k);
            if (functionWords != null && functionWords.contains(sourceWord)) {
              continue;
            }
            sourceWords.add(spans.get(j).getTokText().get(k));
          }
        }
        List<List<String>> translationWords = new ArrayList<List<String>>();
        for (int k = 0; k < sourceWords.size(); k++) {
          String sourceWord = sourceWords.get(k);
          List<String> targets = new ArrayList<String>();
          targets.add(processForMatch(sourceWord));
          List<String> translations = lexTrans.get(sourceWord);
          if (translations != null) {
            for (int m = 0; m < translations.size(); m++) {
              String targetWord = translations.get(m);
              String lookupSource = sourceWord;
              String lookupTarget = targetWord;
              if (!isFw) {
                lookupSource = targetWord;
                lookupTarget = sourceWord;
              }
              double jointCount = lexCounts.getJointCount(lookupSource, lookupTarget);
              double fwProb = lexCounts.getFwProb(lookupSource, lookupTarget);
              double bwProb = lexCounts.getBwProb(lookupSource, lookupTarget);
              if (jointCount >= minMatchCount && fwProb >= minMatchProb && bwProb >= minMatchProb) {
                targets.add(targetWord);
              }
            }
          }
          translationWords.add(targets);
        }
        SortedMap<String, List<Integer>> targetToIndexes = new TreeMap<String, List<Integer>>();
        for (int k = 0; k < translationWords.size(); k++) {
          for (int m = 0; m < translationWords.get(k).size(); m++) {
            String targetWord = translationWords.get(k).get(m);
            List<Integer> indexes = targetToIndexes.get(targetWord);
            if (indexes == null) {
              indexes = new ArrayList<Integer>();
              targetToIndexes.put(targetWord, indexes);
            }
            indexes.add(k);
          }
        }
        String[] targetWords = new String[targetToIndexes.size()];
        int[][] matchIds = new int[targetToIndexes.size()][];
        int index = 0;
        for (Map.Entry<String, List<Integer>> e : targetToIndexes.entrySet()) {
          targetWords[index] = e.getKey();
          List<Integer> sourceIndexeList = e.getValue();
          matchIds[index] = new int[sourceIndexeList.size()];
          for (int k = 0; k < sourceIndexeList.size(); k++) {
            matchIds[index][k] = sourceIndexeList.get(k);
          }
          index++;
        }
        boolean[] doesMatch = new boolean[sourceWords.size()];
        TranslationSet ts = new TranslationSet(targetWords, matchIds, doesMatch, wordCount);
        // System.out.println(ts.toString());
        allTransSets[end][end - i] = ts;
      }
    }
    return allTransSets;
  }

  private double computeErrorCount(TranslationSet translations, String[] targetWords) {
    if (translations.getDoesMatch().length == 0) {
      return 0.0;
    }
    int i = 0;
    int j = 0;
    boolean[] doesMatch = translations.getDoesMatch();
    for (int k = 0; k < doesMatch.length; k++) {
      doesMatch[k] = false;
    }
    String[] transWords = translations.getTargetWords();
    while (i < transWords.length && j < targetWords.length) {
      int cmp = transWords[i].compareTo(targetWords[j]);
      if (cmp == 0) {
        for (int k = 0; k < translations.getMatchIds()[i].length; k++) {
          int index = translations.getMatchIds()[i][k];
          doesMatch[index] = true;
        }
        i++;
        j++;
      } else if (cmp < 0) {
        i++;
      } else {
        j++;
      }
    }
    double matchCount = 0.0;
    for (int k = 0; k < doesMatch.length; k++) {
      if (doesMatch[k]) {
        matchCount += 1.0;
      }
    }
    double matchRatio = matchCount / (double) translations.getDoesMatch().length;
    double errorRatio = 1.0 - matchRatio;
    double errorCount = translations.getWordCount() * errorRatio;
    return errorCount;
  }

  private double getSimScore(SpanRange spanRange, List<LabelSpan> labelSpans,
      List<List<String>> tokenizedLines) {
    List<String> sourceWords = new ArrayList<String>();
    List<String> targetWords = new ArrayList<String>();
    for (int i = spanRange.getSourceStart(); i <= spanRange.getSourceEnd(); i++) {
      int start = labelSpans.get(i).getStart();
      int end = labelSpans.get(i).getEnd();
      for (int j = start; j <= end; j++) {
        sourceWords.addAll(tokenizedLines.get(j));
      }
    }
    for (int i = spanRange.getTargetStart(); i <= spanRange.getTargetEnd(); i++) {
      int start = labelSpans.get(i).getStart();
      int end = labelSpans.get(i).getEnd();
      for (int j = start; j <= end; j++) {
        targetWords.addAll(tokenizedLines.get(j));
      }
    }

    double sourceTokenCount =
        (double) sourceWords.size() * sourceToTargetRatioMultiplier + tokenSmoothing;
    double targetTokenCount = (double) sourceWords.size() + tokenSmoothing;
    double ratio =
        Math.max(sourceTokenCount / targetTokenCount, targetTokenCount / sourceTokenCount);
    if (ratio > maxTokenRatio) {
      return 0.0;
    }
    double fwProb = getMatchScore(sourceWords, targetWords, true);
    double bwProb = getMatchScore(targetWords, sourceWords, false);
    double totalProb = Math.sqrt(fwProb*bwProb);
//    System.out.println("***");
//    System.out.println(StringUtils.join(sourceWords, " "));
//    System.out.println(StringUtils.join(targetWords, " "));
//    System.out.println("fwProb: "+fwProb+", bwProb: "+bwProb);
//    System.out.println("");
    if (totalProb >= minMatchToKeep) {
      return 1.0;
    }
    return 0.0;
  }

  private double getMatchScore(List<String> sourceWords, List<String> targetWords, boolean isFw) {
    double goodCount = 0.0;
    double totalCount = 0.0;
    for (int i = 0; i < sourceWords.size(); i++) {
      String source = sourceWords.get(i);
      if (functionWords != null && functionWords.contains(source)) {
        continue;
      }
      boolean isGood = false;
      for (int j = 0; j < targetWords.size(); j++) {
        String target = targetWords.get(j);
        if (functionWords != null && functionWords.contains(target)) {
          continue;
        }
        if (source.equals(target)) {
          isGood = true;
        }
        String lookupSource = source;
        String lookupTarget = target;
        if (!isFw) {
          lookupSource = target;
          lookupTarget = source;
        }
        double jointCount = lexCounts.getJointCount(lookupSource, lookupTarget);
        double fwProb = lexCounts.getFwProb(lookupSource, lookupTarget);
        double bwProb = lexCounts.getBwProb(lookupSource, lookupTarget);
        if (jointCount >= minMatchCount && fwProb >= minMatchProb && bwProb >= minMatchProb) {
          isGood = true;
        }
      }
      if (isGood) {
        goodCount += 1.0;
      }
      totalCount += 1.0;
    }
    if (totalCount == 0.0) {
      return 0.0;
    }
    double goodProb = goodCount / totalCount;
    return goodProb;
  }

  private boolean passPreFilter(List<TextSpan> sourceSpans, int sourceStart, int sourceEnd,
      List<TextSpan> targetSpans, int targetStart, int targetEnd) {
    int sourceLength = 0;
    for (int i = sourceStart; i <= sourceEnd; i++) {
      sourceLength += sourceSpans.get(i).getTokText().size();
    }

    int targetLength = 0;
    for (int i = targetStart; i <= targetEnd; i++) {
      targetLength += targetSpans.get(i).getTokText().size();
    }

    double sourceCount = sourceToTargetRatioMultiplier * (double) sourceLength + tokenSmoothing;
    double targetCount = (double) targetLength + tokenSmoothing;

    double ratio = Math.max(sourceCount / targetCount, targetCount / sourceCount);
    if (ratio >= maxTokenRatio) {
      return false;
    }
    return true;
  }

  private String getRawText(List<TextSpan> spans, int start, int end) {
    StringBuilder sb = new StringBuilder();
    for (int i = start; i <= end; i++) {
      if (i > start) {
        sb.append(" ");
      }
      sb.append(spans.get(i).getRawText());
    }
    return sb.toString();
  }

  private String getTokenizedText(List<TextSpan> spans, int start, int end) {
    List<String> tokens = new ArrayList<String>();
    for (int i = start; i <= end; i++) {
      tokens.addAll(spans.get(i).getTokText());
    }
    return StringUtils.join(tokens, " ");
  }

  private double getNullScore(List<TextSpan> spans, int i) {
    double nullScorePenalty = 0.9;
    double score = nullScorePenalty * (double) spans.get(i).getTokText().size();
    return score;
  }

  public class MatchCount {
    @Override
    public String toString() {
      return "MatchCount [goodCount=" + goodCount + ", medCount=" + medCount + ", totalCount="
          + totalCount + ", wordCount=" + wordCount + "]";
    }

    public double getWordCount() {
      return wordCount;
    }

    public void setWordCount(double wordCount) {
      this.wordCount = wordCount;
    }

    public double getGoodCount() {
      return goodCount;
    }

    public void setGoodCount(double goodCount) {
      this.goodCount = goodCount;
    }

    public double getMedCount() {
      return medCount;
    }

    public void setMedCount(double medCount) {
      this.medCount = medCount;
    }

    public double getTotalCount() {
      return totalCount;
    }

    public void setTotalCount(double totalCount) {
      this.totalCount = totalCount;
    }

    private double goodCount;
    private double medCount;
    private double totalCount;
    private double wordCount;

    public MatchCount(double goodCount, double medCount, double totalCount, double wordCount) {
      super();
      this.goodCount = goodCount;
      this.medCount = medCount;
      this.totalCount = totalCount;
      this.wordCount = wordCount;
    }

    public double getErrorScore() {
      if (totalCount == 0.0) {
        return 0.0;
      }
      double matchRate = (goodCount + medCount) / totalCount;
      double errorScore = wordCount * (1.0 - matchRate);
      return errorScore;
    }
  }

  private String cleanupString(String input) {
    String str = input;
    str = str.replaceAll("\\.\\s*\\.\\s*\\.(((\\.)|(\\s))+)", " ... ");
    str = str.replaceAll("\\s+", " ");
    str = str.replaceAll("(\n|\r|\u0085|\u2028|\u2029)", " ");
    str = str.replaceAll("^\\s+", "");
    str = str.replaceAll("\\s+$", "");
    return str;
  }

  private List<TextSpan> getSpans(List<String> lines, Tokenizer tokenizer) {
    int maxLinesPerGroup = 30;
    List<TextSpan> spans = new ArrayList<TextSpan>();
    boolean newGroup = true;
    int numLinesInGroup = 0;
    List<Range> ranges = new ArrayList<Range>();
    for (int i = 0; i < lines.size(); i++) {
      String line = lines.get(i);
      line = cleanupString(line);
      if (line.length() == 0 || numLinesInGroup >= maxLinesPerGroup || isNewGroup(line)) {
        newGroup = true;
        numLinesInGroup = 0;
      }
      if (newGroup) {
        ranges.add(new Range(i, i));
      }
      newGroup = false;
      Range range = ranges.get(ranges.size() - 1);
      range.setEnd(i);
      numLinesInGroup++;
    }
    for (Range range : ranges) {
      StringBuilder sb = new StringBuilder();
      for (int j = range.getStart(); j <= range.getEnd(); j++) {
        String line = lines.get(j);
        sb.append(" ");
        sb.append(line);
      }
      String rawText = sb.toString();
      rawText = StringUtils.trim(rawText);
      rawText = rawText.replaceAll("\\s+", " ");
      TokenAlignment alignment = new TokenAlignment();
      List<String> tokText = tokenizer.tokenize(rawText, alignment);
      List<Range> segments = sentenceSegmentor.segmentSentence(tokText);
      for (int i = 0; i < segments.size(); i++) {
        String splitRawText = getSplitText(rawText, tokText, alignment, segments.get(i));
        TokenAlignment splitAlignment = new TokenAlignment();
        List<String> splitTokText = tokenizer.tokenize(splitRawText, splitAlignment);
        TextSpan span =
            new TextSpan(range.getStart(), i, splitRawText, splitTokText, splitAlignment);
        spans.add(span);
      }
    }
    return spans;
  }

  private boolean isNewGroup(String line) {
    Matcher m = newGroupPattern.matcher(line);
    if (m.find()) {
      return true;
    }
    return false;
  }

  private List<TextSegment> extractSegments(String fileId, List<String> lines) {
    int segmentIndex = 0;
    List<TextSegment> segments = new ArrayList<TextSegment>();
    List<String> cleanLines = new ArrayList<String>();
    for (String line : lines) {
      cleanLines.add(cleanupString(line));
    }
    List<LineLabel> lineLabels = new ArrayList<LineLabel>();
    for (int i = 0; i < cleanLines.size(); i++) {
      String line = cleanLines.get(i);
      LineLabel label = LineLabel.BLANK;
      if (line.length() > 0) {
        int sourceCharCount = 0;
        int targetCharCount = 0;
        for (int j = 0; j < line.length(); j++) {
          char c = line.charAt(j);
          if (sourceTokenizer.isLanguageChar(c)) {
            sourceCharCount++;
          } else if (targetTokenizer.isLanguageChar(c)) {
            targetCharCount++;
          }
        }
        int totalCount = sourceCharCount + targetCharCount;
        if (totalCount > 0) {
          double sourcePercent = (double) sourceCharCount / (double) totalCount;
          double targetPercent = (double) targetCharCount / (double) totalCount;
          if (targetPercent >= minTargetPercent) {
            label = LineLabel.TARGET;
          } else if (sourcePercent >= minSourcePercent) {
            label = LineLabel.SOURCE;
          }
        }
      }
      lineLabels.add(label);
    }
    List<LabelSpan> labelSpans = new ArrayList<LabelSpan>();
    for (int i = 0; i < lineLabels.size(); i++) {
      if (i == 0) {
        labelSpans.add(new LabelSpan(0, 0, lineLabels.get(i)));
      } else {
        if (lineLabels.get(i) == lineLabels.get(i - 1)) {
          labelSpans.get(labelSpans.size() - 1).setEnd(i);
        } else {
          labelSpans.add(new LabelSpan(i, i, lineLabels.get(i)));
        }
      }
    }
    List<List<String>> tokenizedLines = new ArrayList<List<String>>();
    for (int i = 0; i < cleanLines.size(); i++) {
      String line = cleanLines.get(i);
      List<String> tokens = null;
      LineLabel label = lineLabels.get(i);
      if (label == LineLabel.SOURCE) {
        tokens = sourceTokenizer.tokenize(line);
      } else if (label == LineLabel.TARGET) {
        tokens = targetTokenizer.tokenize(line);
      } else {
        tokens = new ArrayList<String>();
      }
      tokenizedLines.add(tokens);
    }
    for (int i = 0; i < labelSpans.size(); i++) {
      LabelSpan labelSpan = labelSpans.get(i);
      LineLabel label = labelSpan.getLabel();
      int start = labelSpan.getStart();
      int end = labelSpan.getEnd();
      System.out.println("Label Span [" + i + "]: " + start + " " + end + " " + label);
      for (int j = start; j <= end; j++) {
        System.out.println(tokenizedLines.get(j));
      }
      if (label == LineLabel.SOURCE) {
        List<SpanRange> spanCands = getSpanCandidates(labelSpans, i);
        if (spanCands != null && spanCands.size() > 0) {
          List<Double> simScores = new ArrayList<Double>();
          for (int j = 0; j < spanCands.size(); j++) {
            double score = getSimScore(spanCands.get(j), labelSpans, tokenizedLines);
            simScores.add(score);
          }
          double bestScore = 0.0;
          int bestIndex = -1;
          for (int j = 0; j < spanCands.size(); j++) {
            double score = simScores.get(j);
            if (j == 0 || score > bestScore) {
              bestScore = score;
              bestIndex = j;
            }
          }
          if (bestScore > 0.0) {
            SpanRange bestSpanRange = spanCands.get(bestIndex);
            Range sourceLineRange =
                getLineRange(labelSpans, bestSpanRange.getSourceStart(), bestSpanRange
                    .getSourceEnd());
            String rawSource =
                getJoinedLines(cleanLines, sourceLineRange.getStart(), sourceLineRange.getEnd());
            Range targetLineRange =
                getLineRange(labelSpans, bestSpanRange.getTargetStart(), bestSpanRange
                    .getTargetEnd());
            String rawTarget =
                getJoinedLines(cleanLines, targetLineRange.getStart(), targetLineRange.getEnd());
            TextSegment segment = new TextSegment();
            String guid =
                String.format("[ExtractedSegments][File_%s][Segment_%05d]", fileId, segmentIndex);
            segment.insert("GUID", guid);
            segment.insert("RAW_SOURCE", rawSource);
            segment.insert("RAW_TARGET", rawTarget);
            segment.insert("TOKENIZED_SOURCE", StringUtils.join(
                sourceTokenizer.tokenize(rawSource), " "));
            segment.insert("TOKENIZED_TARGET", StringUtils.join(
                targetTokenizer.tokenize(rawTarget), " "));
            segments.add(segment);
            segmentIndex++;
          }
        }
      }
    }
    return segments;
  }

  //
  // private List<SpanRange> getSpanCandidates(List<LabelSpan> labelSpans, int index) {
  // List<SpanRange> spanCands = new ArrayList<SpanRange>();
  // boolean isValid = false;
  // int sourceStart = -1;
  // for (int i = index + 1; i < labelSpans.size(); i++) {
  // LineLabel label = labelSpans.get(i).getLabel();
  // if (label == LineLabel.SOURCE) {
  // isValid = true;
  // sourceStart = i;
  // break;
  // } else if (label == LineLabel.TARGET) {
  // break;
  // }
  // }
  // if (!isValid) {
  // return null;
  // }
  // int targetStart = index;
  // int targetEnd = index;
  // int sourceEnd = sourceStart;
  // for (int i = targetStart - 1; i >= 0; i--) {
  // LineLabel label = labelSpans.get(i).getLabel();
  // if (label == LineLabel.TARGET) {
  // targetStart = i;
  // break;
  // } else if (label == LineLabel.SOURCE) {
  // break;
  // }
  // }
  // for (int i = sourceStart; i < labelSpans.size(); i++) {
  // LineLabel label = labelSpans.get(i).getLabel();
  // if (label == LineLabel.SOURCE) {
  // sourceEnd = i;
  // break;
  // } else if (label == LineLabel.TARGET) {
  // break;
  // }
  // }
  // for (int i = targetStart; i <= targetEnd; i++) {
  // for (int j = sourceStart; j <= sourceEnd; j++) {
  // SpanRange spanRange = new SpanRange(sourceStart, j, i, targetEnd);
  // spanCands.add(spanRange);
  // }
  // }
  // return spanCands;
  // }

  private List<SpanRange> getSpanCandidates(List<LabelSpan> labelSpans, int index) {
    List<SpanRange> spanCands = new ArrayList<SpanRange>();
    boolean isValid = false;
    int targetStart = -1;
    for (int i = index + 1; i < labelSpans.size(); i++) {
      LineLabel label = labelSpans.get(i).getLabel();
      if (label == LineLabel.TARGET) {
        isValid = true;
        targetStart = i;
        break;
      } else if (label == LineLabel.SOURCE) {
        break;
      }
    }
    if (!isValid) {
      return null;
    }
    int sourceStart = index;
    int sourceEnd = index;
    int targetEnd = targetStart;
    for (int i = index - 1; i >= 0; i--) {
      LineLabel label = labelSpans.get(i).getLabel();
      if (label == LineLabel.SOURCE) {
        sourceStart = i;
        break;
      } else if (label == LineLabel.TARGET) {
        break;
      }
    }
    for (int i = targetStart; i < labelSpans.size(); i++) {
      LineLabel label = labelSpans.get(i).getLabel();
      if (label == LineLabel.TARGET) {
        targetEnd = i;
        break;
      } else if (label == LineLabel.SOURCE) {
        break;
      }
    }
    for (int i = sourceStart; i <= sourceEnd; i++) {
      for (int j = targetStart; j <= targetEnd; j++) {
        SpanRange spanRange = new SpanRange(i, sourceEnd, targetStart, j);
        spanCands.add(spanRange);
      }
    }
    return spanCands;
  }

  private Range getLineRange(List<LabelSpan> labelSpans, int spanStart, int spanEnd) {
    int start = labelSpans.get(spanStart).getStart();
    int end = labelSpans.get(spanEnd).getEnd();
    return new Range(start, end);
  }

  private String getJoinedLines(List<String> lines, int start, int end) {
    StringBuilder sb = new StringBuilder();
    for (int i = start; i <= end; i++) {
      if (i > start) {
        sb.append(" ");
      }
      sb.append(lines.get(i));
    }
    return cleanupString(sb.toString());
  }

  private String getSplitText(String rawText, List<String> tokText, TokenAlignment rawToTokAlign,
      Range range) {
    Map<Integer, List<Range>> tokIndexToRawRanges = new HashMap<Integer, List<Range>>();
    for (AlignmentPair p : rawToTokAlign) {
      int tokStartIndex = p.getOutput().getStart();
      int tokEndIndex = p.getOutput().getEnd();
      for (int i = tokStartIndex; i <= tokEndIndex; i++) {
        List<Range> ranges = tokIndexToRawRanges.get(i);
        if (ranges == null) {
          ranges = new ArrayList<Range>();
          tokIndexToRawRanges.put(i, ranges);
        }
        ranges.add(p.getInput());
      }
    }
    int charStart = -1;
    int charEnd = -1;
    for (int i = range.getStart(); i <= range.getEnd(); i++) {
      List<Range> ranges = tokIndexToRawRanges.get(i);
      if (ranges != null) {
        for (Range rawCharRange : ranges) {
          if (charStart == -1 || rawCharRange.getStart() < charStart) {
            charStart = rawCharRange.getStart();
          }
          if (charEnd == -1 || rawCharRange.getEnd() > charEnd) {
            charEnd = rawCharRange.getEnd();
          }
        }
      }
    }
    String splitRawText = rawText.substring(charStart, charEnd + 1);
    return splitRawText;
  }
}
