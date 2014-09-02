package edu.isi.madcat.tmem.translation;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import edu.isi.madcat.tmem.alignment.AlignmentGenerator;
import edu.isi.madcat.tmem.alignment.AlignmentPair;
import edu.isi.madcat.tmem.alignment.Range;
import edu.isi.madcat.tmem.alignment.TokenAlignment;
import edu.isi.madcat.tmem.alignment.WerAlignmentScorer;
import edu.isi.madcat.tmem.backend.BackendRequestor;
import edu.isi.madcat.tmem.backend.messages.AuxiliaryRule;
import edu.isi.madcat.tmem.backend.messages.BiasedTranslation;
import edu.isi.madcat.tmem.backend.messages.SegmentorRequest;
import edu.isi.madcat.tmem.backend.messages.SegmentorResponse;
import edu.isi.madcat.tmem.backend.messages.TokenizerRequest;
import edu.isi.madcat.tmem.backend.messages.TokenizerResponse;
import edu.isi.madcat.tmem.backend.messages.TranslationHypothesis;
import edu.isi.madcat.tmem.backend.messages.TranslationRequest;
import edu.isi.madcat.tmem.backend.messages.TranslationResponse;
import edu.isi.madcat.tmem.backend.messages.WordAlignment;
import edu.isi.madcat.tmem.exceptions.CateProcessException;
import edu.isi.madcat.tmem.lextrans.LexTranslator;
import edu.isi.madcat.tmem.lookup.QueryHash;
import edu.isi.madcat.tmem.lookup.api.UserDictionaryResult;
import edu.isi.madcat.tmem.processors.HierarchicalOutput;
import edu.isi.madcat.tmem.processors.HierarchicalProcessor;
import edu.isi.madcat.tmem.processors.SubstringOutput;
import edu.isi.madcat.tmem.processors.SubstringOutput.Replacement;
import edu.isi.madcat.tmem.processors.SubstringProcessor;
import edu.isi.madcat.tmem.sql.SqlHandler;
import edu.isi.madcat.tmem.sql.SqlManager;
import edu.isi.madcat.tmem.tokenize.Tokenizer;
import edu.isi.madcat.tmem.tokenize.TokenizerFactory;

public class TranslationHelper {

  class MyTokenizerResponse {
    private TokenizerResponse response;

    private TokenAlignment alignment;

    public MyTokenizerResponse() {
      this.response = null;
      this.alignment = null;
    }

    public MyTokenizerResponse(TokenizerResponse response) {
      this.response = response;
      this.alignment = null;
    }

    public TokenAlignment getAlignment() {
      return alignment;
    }

    public TokenizerResponse getResponse() {
      return response;
    }

    public void setAlignment(TokenAlignment alignment) {
      this.alignment = alignment;
    }

    public void setResponse(TokenizerResponse response) {
      this.response = response;
    }
  }

  protected BackendRequestor backendRequestor;

  protected String sourceLanguage;

  protected String targetLanguage;

  protected SubstringProcessor substringProcessor;

  protected SubstringProcessor tokenProcessor;

  protected Tokenizer sourceTokenizer;

  protected Tokenizer singleCharTokenizer;

  protected boolean doCharAlignTarget;

  protected SqlManager sqlManager;

  protected int maxRecursionLevel;

  protected LexTranslator lexTranslator;

  protected HierarchicalProcessor hierarchicalProcessor;

  public TranslationHelper(String sourceLanguage, String targetLanguage,
      BackendRequestor backendRequestor) {
    this.sourceLanguage = sourceLanguage;
    this.targetLanguage = targetLanguage;
    this.backendRequestor = backendRequestor;
    this.singleCharTokenizer = TokenizerFactory.create("single_character");
    this.doCharAlignTarget = false;
    this.maxRecursionLevel = 2;
  }

  public BackendRequestor getBackendRequestor() {
    return backendRequestor;
  }

  public HierarchicalProcessor getHierarchicalProcessor() {
    return hierarchicalProcessor;
  }

  public LexTranslator getLexTranslator() {
    return lexTranslator;
  }

  public int getMaxRecursionLevel() {
    return maxRecursionLevel;
  }

  public TokenAlignment getRawSourceToTokAlignment(String inputString, List<String> tokens) {
    return getCharToWordAlignment(inputString, tokens);
  }

  public Tokenizer getSingleCharTokenizer() {
    return singleCharTokenizer;
  }

  public String getSourceLanguage() {
    return sourceLanguage;
  }

  public Tokenizer getSourceTokenizer() {
    return sourceTokenizer;
  }

  public SqlManager getSqlManager() {
    return sqlManager;
  }

  public SubstringProcessor getSubstringProcessor() {
    return substringProcessor;
  }

  public String getTargetLanguage() {
    return targetLanguage;
  }

  public TokenAlignment getTargetTokToDetokAlignment(List<String> detokenizedTranslation,
      List<String> tokenizedTranslation) {
    TokenAlignment trgTokToTrgDetokAlign = null;
    if (doCharAlignTarget) {
      trgTokToTrgDetokAlign =
          getCharToWordAlignment(tokenizedTranslation, StringUtils
              .join(detokenizedTranslation, " "));
    } else {
      trgTokToTrgDetokAlign =
          AlignmentGenerator.wordToWordAlignment(tokenizedTranslation, detokenizedTranslation);
    }
    return trgTokToTrgDetokAlign;
  }

  public SubstringProcessor getTokenProcessor() {
    return tokenProcessor;
  }

  public boolean isDoCharAlignTarget() {
    return doCharAlignTarget;
  }

  public void setBackendRequestor(BackendRequestor backendRequestor) {
    this.backendRequestor = backendRequestor;
  }

  public void setDoCharAlignTarget(boolean doCharAlignTarget) {
    this.doCharAlignTarget = doCharAlignTarget;
  }

  public void setHierarchicalProcessor(HierarchicalProcessor hierarchicalProcessor) {
    this.hierarchicalProcessor = hierarchicalProcessor;
  }

  public void setLexTranslator(LexTranslator lexTranslator) {
    this.lexTranslator = lexTranslator;
  }

  public void setMaxRecursionLevel(int maxRecursionLevel) {
    this.maxRecursionLevel = maxRecursionLevel;
  }

  public void setSingleCharTokenizer(Tokenizer singleCharTokenizer) {
    this.singleCharTokenizer = singleCharTokenizer;
  }

  public void setSourceLanguage(String sourceLanguage) {
    this.sourceLanguage = sourceLanguage;
  }

  public void setSourceTokenizer(Tokenizer sourceTokenizer) {
    this.sourceTokenizer = sourceTokenizer;
  }

  public void setSqlManager(SqlManager sqlManager) {
    this.sqlManager = sqlManager;
  }

  public void setSubstringProcessor(SubstringProcessor substringProcessor) {
    this.substringProcessor = substringProcessor;
  }

  public void setTargetLanguage(String targetLanguage) {
    this.targetLanguage = targetLanguage;
  }

  public void setTokenProcessor(SubstringProcessor tokenProcessor) {
    this.tokenProcessor = tokenProcessor;
  }

  public TranslatedSegment translate(SourceSegment segment, UserTranslationInfo userInfo)
      throws CateProcessException {
    return translate(segment, userInfo, 0);
  }

  public TranslatedSegment translate(SourceSegment segment, UserTranslationInfo userInfo,
      int recursionLevel) throws CateProcessException {
    // String origSourceText = Utils.processWhitespace(segment.getText());
    String origSourceText = segment.getText();

    HierarchicalOutput hierOutput = null;
    if (recursionLevel < maxRecursionLevel && hierarchicalProcessor != null) {
      hierOutput = hierarchicalProcessor.processString(origSourceText);
    }
    if (hierOutput != null) {
      List<TranslatedSegment> outputSegments = new ArrayList<TranslatedSegment>();
      for (int i = 0; i < hierOutput.getValues().size(); i++) {
        HierarchicalOutput.OutputValue value = hierOutput.getValues().get(i);
        if (value.getType() == HierarchicalOutput.OutputType.TRANSLATE) {
          SourceSegment subSegment = new SourceSegment(segment.getId(), value.getText());
          TranslatedSegment outputSegment = translate(subSegment, userInfo, recursionLevel + 1);
          outputSegments.add(outputSegment);
        }
      }
      return joinSegments(segment, hierOutput, outputSegments);
    } else {
      return translateNonHier(segment, userInfo);
    }
  }

  public TranslatedSegment translateNonHier(SourceSegment segment, UserTranslationInfo userInfo)
      throws CateProcessException {
    List<Integer> whitespaceOffset = new ArrayList<Integer>();
    String rawSourceText = removeWhitespace(segment.getText(), whitespaceOffset);
    if (rawSourceText.length() == 0) {
      return TranslatedSegment.createEmptySegment(segment.getId());
    }
    SubstringOutput substringOutput = null;

    String sourceText = rawSourceText;
    if (substringProcessor != null) {
      substringOutput = substringProcessor.processString(sourceText);
      sourceText = substringOutput.getMarkedText();
    }

    MyTokenizerResponse tokenizerResponse = tokenizeSource(sourceLanguage, sourceText);
    if (tokenizerResponse == null) {
      return null;
    }

    List<String> tokenizedWords = tokenizerResponse.getResponse().getTokens();

    List<String> tokenizedTranslation = new ArrayList<String>();
    List<String> detokenizedTranslation = new ArrayList<String>();
    TokenAlignment srcTokToTrgTokAlign = new TokenAlignment();
    List<String> processedTokenizedWords = null;
    List<SubstringOutput> tokenProcessorOutput = null;

    boolean isLexTranslation = false;
    if (lexTranslator != null) {
      if (tokenizedWords.size() <= lexTranslator.getMaxSegmentLength()) {
        TranslationRequest request =
            new TranslationRequest(tokenizedWords, new ArrayList<AuxiliaryRule>(),
                new ArrayList<BiasedTranslation>(), new Boolean(false), new Long(1));      
        TranslationResponse response = lexTranslator.translate(request);
        if (response != null) {
          TranslationHypothesis onebest = response.getHypotheses().get(0);
          tokenizedTranslation = onebest.getTranslatedWords();
          detokenizedTranslation = onebest.getDetokenizedWords();
          srcTokToTrgTokAlign = new TokenAlignment(onebest.getWordAlignment());
          processedTokenizedWords = tokenizedWords;
          isLexTranslation = true;
        }
      }
    }
    

    if (!isLexTranslation) {
      if (tokenProcessor != null) {
        processedTokenizedWords = new ArrayList<String>();
        tokenProcessorOutput = new ArrayList<SubstringOutput>();
        for (int i = 0; i < tokenizedWords.size(); i++) {
          String token = tokenizedWords.get(i);
          SubstringOutput output = tokenProcessor.processString(token);
          tokenProcessorOutput.add(output);
          processedTokenizedWords.add(output.getMarkedText());
        }
      } else {
        processedTokenizedWords = tokenizedWords;
      }
      boolean isPartialTranslation = detectPartialTranslation(processedTokenizedWords);

      SegmentorRequest segmentorRequest = new SegmentorRequest(processedTokenizedWords);
      SegmentorResponse segmentorResponse = backendRequestor.segment(segmentorRequest);
      List<List<String>> splitSourceTokens =
          segmentorResponse.generateSplitSegments(segmentorRequest);
      int sourceOffset = 0;
      int targetOffset = 0;
      for (List<String> sourceTokens : splitSourceTokens) {
        List<AuxiliaryRule> auxiliaryRules = getAuxiliaryRules(sourceTokens, userInfo);
        List<BiasedTranslation> forcedTranslations = new ArrayList<BiasedTranslation>();
        TranslationRequest translationRequest =
            new TranslationRequest(sourceTokens, auxiliaryRules, forcedTranslations,
                isPartialTranslation, new Long(1));

        TranslationResponse translationResponse = backendRequestor.translate(translationRequest);
        TranslationHypothesis onebest = translationResponse.getHypotheses().get(0);
        TokenAlignment mtAlignment = new TokenAlignment(onebest.getWordAlignment());
        tokenizedTranslation.addAll(onebest.getTranslatedWords());
        detokenizedTranslation.addAll(onebest.getDetokenizedWords());
        srcTokToTrgTokAlign.extendAlignment(sourceOffset, targetOffset, mtAlignment);
        sourceOffset += sourceTokens.size();
        targetOffset += onebest.getTranslatedWords().size();
      }
    }
    
    List<String> replTokenizedTrans = tokenizedTranslation;
    List<String> replDetokTrans = detokenizedTranslation;

    List<Replacement> replacementList = new ArrayList<Replacement>();
    if (substringOutput != null) {
      replacementList.addAll(substringOutput.getReplacementList());
    }
    
    if (tokenProcessorOutput != null) {
      for (SubstringOutput output : tokenProcessorOutput) {
        replacementList.addAll(output.getReplacementList());
      }
    }

    if (replacementList.size() > 0) {
      replTokenizedTrans = SubstringOutput.replaceTokens(tokenizedTranslation, replacementList);
      replDetokTrans = SubstringOutput.replaceTokens(detokenizedTranslation, replacementList);
    }

    List<String> replTokenizedWords =
        SubstringOutput.replaceTokens(tokenizedWords, replacementList);

    TokenAlignment srcRawToTokAlign = getCharToWordAlignment(rawSourceText, replTokenizedWords);
    // if (tokenizerResponse.getAlignment() != null) {
    // srcRawToTokAlign = tokenizerResponse.getAlignment();
    // } else {
    // srcRawToTokAlign = getCharToWordAlignment(origSourceText, tokenizedWords);
    // }

    TokenAlignment trgTokToTrgDetokAlign =
        getTargetTokToDetokAlignment(replDetokTrans, replTokenizedTrans);

    TokenAlignment srcTokToTrgDetokAlign =
        TokenAlignment.projectAlignment(srcTokToTrgTokAlign, trgTokToTrgDetokAlign);

    TokenAlignment srcRawToTrgDetokAlign =
        TokenAlignment.projectAlignment(srcRawToTokAlign, srcTokToTrgDetokAlign);

    srcRawToTokAlign = addWhitespaceOffset(srcRawToTokAlign, whitespaceOffset);
    srcRawToTrgDetokAlign = addWhitespaceOffset(srcRawToTrgDetokAlign, whitespaceOffset);

    List<OutputToken> outputTokens = new ArrayList<OutputToken>();
    for (String word : replDetokTrans) {
      outputTokens.add(new OutputToken(word));
    }
    TranslatedSegment translatedSegment =
        new TranslatedSegment(segment.getId(), segment.getText(), replTokenizedWords,
            replTokenizedTrans, outputTokens, srcRawToTokAlign, srcTokToTrgTokAlign,
            srcTokToTrgDetokAlign, srcRawToTrgDetokAlign, trgTokToTrgDetokAlign);

    return translatedSegment;
  }

  public TranslationResponse translatePhrase(String line, int nbestSize)
      throws CateProcessException {
    SourceSegment segment = new SourceSegment(0, line);
    TokenizerRequest tokenizerRequest = new TokenizerRequest(sourceLanguage, segment.getText());
    TokenizerResponse tokenizerResponse = backendRequestor.tokenize(tokenizerRequest);
    SegmentorRequest segmentorRequest = new SegmentorRequest(tokenizerResponse.getTokens());
    SegmentorResponse segmentorResponse = backendRequestor.segment(segmentorRequest);
    List<List<String>> splitSourceTokens =
        segmentorResponse.generateSplitSegments(segmentorRequest);
    if (splitSourceTokens.size() != 1) {
      return null;
    }
    List<AuxiliaryRule> auxiliaryRules = new ArrayList<AuxiliaryRule>();
    List<BiasedTranslation> forcedTranslations = new ArrayList<BiasedTranslation>();
    TranslationRequest translationRequest =
        new TranslationRequest(splitSourceTokens.get(0), auxiliaryRules, forcedTranslations,
            new Boolean(true), new Long(nbestSize));
    TranslationResponse translationResponse = backendRequestor.translate(translationRequest);
    return translationResponse;
  }

  public RawTranslationResult translateWithRawResults(SourceSegment segment, int nbestSize)
      throws CateProcessException {
    RawTranslationResult result = new RawTranslationResult();

    String sourceText = segment.getText();

    TokenizerRequest tokenizerRequest = new TokenizerRequest(sourceLanguage, sourceText);
    TokenizerResponse tokenizerResponse = backendRequestor.tokenize(tokenizerRequest);
    result.setTokenizerRequest(tokenizerRequest);
    result.setTokenizerResponse(tokenizerResponse);

    if (tokenizerResponse.getTokens().size() > 0) {
      boolean isPartialTranslation = detectPartialTranslation(tokenizerResponse.getTokens());
      SegmentorRequest segmentorRequest = new SegmentorRequest(tokenizerResponse.getTokens());
      SegmentorResponse segmentorResponse = backendRequestor.segment(segmentorRequest);
      result.setSegmentorRequest(segmentorRequest);
      result.setSegmentorResponse(segmentorResponse);

      List<List<String>> splitSourceTokens =
          segmentorResponse.generateSplitSegments(segmentorRequest);

      result.setTranslationRequests(new ArrayList<TranslationRequest>());
      result.setTranslationResponses(new ArrayList<TranslationResponse>());

      for (List<String> sourceTokens : splitSourceTokens) {
        List<AuxiliaryRule> auxiliaryRules = new ArrayList<AuxiliaryRule>();
        List<BiasedTranslation> forcedTranslations = new ArrayList<BiasedTranslation>();
        TranslationRequest translationRequest =
            new TranslationRequest(sourceTokens, auxiliaryRules, forcedTranslations,
                isPartialTranslation, new Long(nbestSize));
        TranslationResponse translationResponse = backendRequestor.translate(translationRequest);
        result.getTranslationRequests().add(translationRequest);
        result.getTranslationResponses().add(translationResponse);
      }
    }
    return result;
  }

  private void addAlignmentWithOffset(TokenAlignment input, int inputOffset, int outputOffset,
      TokenAlignment output) {
    for (AlignmentPair p : input) {
      output.add(p.getInput().getStart() + inputOffset, p.getInput().getEnd() + inputOffset, p
          .getOutput().getStart()
          + outputOffset, p.getOutput().getEnd() + outputOffset);
    }
  }

  private TokenAlignment addWhitespaceOffset(TokenAlignment alignment, List<Integer> offsets) {
    TokenAlignment outputAlignment = new TokenAlignment(alignment);
    for (AlignmentPair p : outputAlignment) {
      Range r = p.getInput();
      int offset = offsets.get(r.getStart());
      r.setStart(r.getStart() + offset);
      r.setEnd(r.getEnd() + offset);
    }
    return outputAlignment;
  }

  private boolean detectPartialTranslation(List<String> tokens) {
    if (tokens.size() <= 5) {
      return true;
    }
    return false;
  }

  private List<AuxiliaryRule> getAuxiliaryRules(List<String> sourceTokens,
      UserTranslationInfo userInfo) {
    List<AuxiliaryRule> auxRules = new ArrayList<AuxiliaryRule>();
    if (sqlManager == null || userInfo == null
        || (userInfo.getUserIds().size() == 0 && userInfo.getGroupIds().size() == 0)) {
      return auxRules;
    }
    Map<String, Pair<Integer, Integer>> keyToRange = new HashMap<String, Pair<Integer, Integer>>();
    List<String> hashValues = new ArrayList<String>();
    int maxNgramSize = 4;
    for (int i = 0; i < sourceTokens.size(); i++) {
      for (int n = 0; n < maxNgramSize && i + n < sourceTokens.size(); n++) {
        List<String> ngram = sourceTokens.subList(i, i + n + 1);
        String ngramString = StringUtils.join(ngram, ' ');
        String hashValue = QueryHash.getHashString(ngramString);
        hashValues.add(hashValue);
        keyToRange.put(hashValue, new ImmutablePair<Integer, Integer>(i, n));
      }
    }

    List<String> querySets = SqlManager.getQuerySets(hashValues);

    for (String querySet : querySets) {
      String userQuerySet = null;
      if (userInfo.getUserIds().size() > 0) {
        userQuerySet = SqlManager.getQuerySetFromObject(userInfo.getUserIds());
      }
      String groupQuerySet = null;
      if (userInfo.getGroupIds().size() > 0) {
        groupQuerySet = SqlManager.getQuerySetFromObject(userInfo.getGroupIds());
      }

      String filterStatement = null;
      if (userQuerySet != null && groupQuerySet != null) {
        filterStatement =
            "d.user_id IN (" + userQuerySet + ") OR d.group_id IN (" + groupQuerySet + ")";
      } else if (userQuerySet != null) {
        filterStatement = "d.user_id IN (" + userQuerySet + ")";
      } else if (groupQuerySet != null) {
        filterStatement = "d.group_id IN (" + groupQuerySet + ")";
      }
      // @formatter:off
      SqlHandler sqlHandler = sqlManager.createHandler();
      PreparedStatement statement =
          sqlHandler
              .prepareStatement("SELECT "
                  + "l.lookup_key, d.dictionary_id, "
                  + "d.user_id, d.group_id, "
                  + "d.source_term_raw, d.source_term_tok, d.source_acronym_raw, d.source_acronym_tok, "
                  + "d.target_term_raw, d.target_term_tok, d.target_acronym_raw, d.target_acronym_tok, "
                  + "l.does_match_source, l.match_score "
                  + "FROM user_dict_lookup AS l "
                  + "LEFT JOIN user_dictionary AS d ON l.dictionary_id = d.dictionary_id "
                  + "WHERE"
                  + "  (l.lookup_key IN ("+querySet+")) AND "
                  + "  (l.match_score = 0) AND "
                  + "  ("+filterStatement+")");
      // @formatter:on
      List<List<String>> results = sqlHandler.executeQuery(statement);
      sqlHandler.close();

      for (int i = 0; i < results.size(); i++) {
        UserDictionaryResult dictResult = new UserDictionaryResult(results.get(i));
        Pair<Integer, Integer> range = keyToRange.get(dictResult.getLookupKey());
        if (range != null) {
          int sourceStart = range.getLeft();
          int sourceEnd = range.getLeft() + range.getRight();
          List<String> targetWords =
              Arrays.asList(StringUtils.split(dictResult.getTargetTermTok(), ' '));
          List<WordAlignment> alignment = new ArrayList<WordAlignment>();
          double score = -5.0;
          AuxiliaryRule auxRule =
              new AuxiliaryRule(new Long(sourceStart), new Long(sourceEnd), targetWords, alignment,
                  new Double(score));
          auxRules.add(auxRule);
        }
      }
    }
    return auxRules;
  }

  private TokenAlignment getCharToWordAlignment(List<String> inputTokens, String outputString) {
    List<String> outputTokens = singleCharTokenizer.tokenize(outputString);

    WerAlignmentScorer scorer = new WerAlignmentScorer(inputTokens, outputTokens);
    scorer.setMaxTokensPerChunk(20);
    TokenAlignment alignment = AlignmentGenerator.heuristicAlignment(scorer);
    if (alignment == null) {
      alignment = AlignmentGenerator.backoffAlignment(inputTokens, outputTokens);
    }
    return alignment;
  }

  private TokenAlignment getCharToWordAlignment(String inputString, List<String> outputTokens) {
    List<String> inputTokens = singleCharTokenizer.tokenize(inputString);

    WerAlignmentScorer scorer = new WerAlignmentScorer(inputTokens, outputTokens);
    scorer.setMaxTokensPerChunk(20);
    TokenAlignment alignment = AlignmentGenerator.heuristicAlignment(scorer);
    if (alignment == null) {
      alignment = AlignmentGenerator.backoffAlignment(inputTokens, outputTokens);
    }
    return alignment;
  }

  private TranslatedSegment joinSegments(SourceSegment sourceSegment,
      HierarchicalOutput hierOutput, List<TranslatedSegment> outputSegments) {
    StringBuffer inputStringBuffer = new StringBuffer();
    List<String> sourceWords = new ArrayList<String>();
    List<String> tokenizedTargetWords = new ArrayList<String>();
    List<OutputToken> targetWords = new ArrayList<OutputToken>();
    TokenAlignment srcRawToSrcTokAlign = new TokenAlignment();
    TokenAlignment srcTokToTrgTokAlign = new TokenAlignment();
    TokenAlignment srcTokToTrgDetokAlign = new TokenAlignment();
    TokenAlignment srcRawToTrgDetokAlign = new TokenAlignment();
    TokenAlignment trgTokToTrgDetokAlign = new TokenAlignment();
    List<DisplayPhrase> phrases = null;
    Map<Integer, Integer> targetWordToPhrase = null;
    int segmentIndex = 0;
    int numTargetWords = 0;
    int numPhrases = 0;
    int numTokenizedTargetWords = 0;
    int numSourceWords = 0;
    int numInputChars = 0;
    for (int i = 0; i < hierOutput.getValues().size(); i++) {
      HierarchicalOutput.OutputValue value = hierOutput.getValues().get(i);
      if (value.getType() == HierarchicalOutput.OutputType.TRANSLATE) {
        TranslatedSegment segment = outputSegments.get(segmentIndex);
        inputStringBuffer.append(segment.getInputString());
        sourceWords.addAll(segment.getSourceWords());
        tokenizedTargetWords.addAll(segment.getTokenizedTargetWords());
        targetWords.addAll(segment.getTargetWords());
        if (segment.getPhrases() != null) {
          if (phrases == null) {
            phrases = new ArrayList<DisplayPhrase>();
          }
          phrases.addAll(segment.getPhrases());
        }
        if (segment.getTargetWordToPhrase() != null) {
          if (targetWordToPhrase == null) {
            targetWordToPhrase = new HashMap<Integer, Integer>();
          }
          for (Map.Entry<Integer, Integer> e : segment.getTargetWordToPhrase().entrySet()) {
            int inputWordIndex = e.getKey();
            int inputPhraseIndex = e.getValue();
            targetWordToPhrase.put(numTargetWords + inputWordIndex, numPhrases + inputPhraseIndex);
          }
        }

        addAlignmentWithOffset(segment.getSrcRawToSrcTokAlign(), numInputChars, numSourceWords,
            srcRawToSrcTokAlign);
        addAlignmentWithOffset(segment.getSrcTokToTrgTokAlign(), numSourceWords,
            numTokenizedTargetWords, srcTokToTrgTokAlign);
        addAlignmentWithOffset(segment.getSrcTokToTrgDetokAlign(), numSourceWords, numTargetWords,
            srcTokToTrgDetokAlign);
        addAlignmentWithOffset(segment.getSrcRawToTrgDetokAlign(), numInputChars, numTargetWords,
            srcRawToTrgDetokAlign);
        addAlignmentWithOffset(segment.getTrgTokToTrgDetokAlign(), numTokenizedTargetWords,
            numTargetWords, trgTokToTrgDetokAlign);

        numInputChars += segment.getInputString().length();
        if (segment.getPhrases() != null) {
          numPhrases += segment.getPhrases().size();
        }
        numTargetWords += segment.getTargetWords().size();
        numSourceWords += segment.getSourceWords().size();
        numTokenizedTargetWords += segment.getTokenizedTargetWords().size();
        segmentIndex++;
      } else if (value.getType() == HierarchicalOutput.OutputType.JOIN) {
        inputStringBuffer.append(value.getText());
        sourceWords.add(value.getText());
        tokenizedTargetWords.add(value.getText());
        targetWords.add(new OutputToken(value.getText(), value.getJoinType()));
        srcRawToSrcTokAlign.add(numInputChars, value.getText().length() - 1 + numInputChars,
            numSourceWords, numSourceWords);
        srcTokToTrgTokAlign.add(numSourceWords, numSourceWords, numTokenizedTargetWords,
            numTokenizedTargetWords);
        srcTokToTrgDetokAlign.add(numSourceWords, numSourceWords, numTargetWords, numTargetWords);
        srcRawToTrgDetokAlign.add(numInputChars, value.getText().length() - 1 + numInputChars,
            numTargetWords, numTargetWords);
        trgTokToTrgDetokAlign.add(numTokenizedTargetWords, numTokenizedTargetWords, numTargetWords,
            numTargetWords);
        numInputChars += value.getText().length();
        numTargetWords += 1;
        numSourceWords += 1;
        numTokenizedTargetWords += 1;
      }
    }

    TranslatedSegment segment =
        new TranslatedSegment(sourceSegment.getId(), inputStringBuffer.toString(), sourceWords,
            tokenizedTargetWords, targetWords, srcRawToSrcTokAlign, srcTokToTrgTokAlign,
            srcTokToTrgDetokAlign, srcRawToTrgDetokAlign, trgTokToTrgDetokAlign);
    if (phrases != null) {
      segment.setPhrases(phrases);
    }
    if (targetWordToPhrase != null) {
      segment.setTargetWordToPhrase(targetWordToPhrase);
    }
    return segment;
  }

  private String removeWhitespace(String text, List<Integer> offsetForOrig) {
    List<String> output = new ArrayList<String>();
    char[] chars = text.toCharArray();
    int lastNonWhitespace = -1;
    int numIgnoredChars = 0;
    boolean[] isWhitespace = new boolean[chars.length];
    for (int i = 0; i < chars.length; i++) {
      char c = chars[i];
      isWhitespace[i] = false;
      if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
        isWhitespace[i] = true;
      }
    }
    for (int i = 0; i < chars.length; i++) {
      boolean isIgnored = true;
      char c = chars[i];
      if (!isWhitespace[i]) {
        // if the last character was whitespace, and there was a non-whitespace character at some
        // point before this, add a space
        output.add("" + c);
        offsetForOrig.add(numIgnoredChars);
        lastNonWhitespace = i;
        isIgnored = false;
      } else if (lastNonWhitespace != -1 && i < chars.length - 1 && !isWhitespace[i + 1]) {
        output.add(" ");
        offsetForOrig.add(numIgnoredChars);
        isIgnored = false;
      }
      if (isIgnored) {
        numIgnoredChars++;
      }
    }
    String outputString = StringUtils.join(output, "");
    return outputString;
  }

  private MyTokenizerResponse tokenizeSource(String sourceLanguage, String sourceText)
      throws CateProcessException {

    if (sourceTokenizer != null) {
      TokenAlignment alignment = new TokenAlignment();
      List<String> tokens = sourceTokenizer.tokenize(sourceText, alignment);
      TokenizerResponse tokenizerResponse = new TokenizerResponse(tokens);
      MyTokenizerResponse response = new MyTokenizerResponse(tokenizerResponse);
      response.setAlignment(alignment);
      return response;
    } else {
      TokenizerRequest tokenizerRequest = new TokenizerRequest(sourceLanguage, sourceText);
      TokenizerResponse tokenizerResponse = backendRequestor.tokenize(tokenizerRequest);
      if (tokenizerResponse.getTokens().size() == 0) {
        return null;
      }
      MyTokenizerResponse response = new MyTokenizerResponse(tokenizerResponse);
      return response;
    }
  }
}
