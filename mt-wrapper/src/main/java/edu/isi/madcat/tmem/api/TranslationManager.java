package edu.isi.madcat.tmem.api;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.InputStreamResource;

import edu.isi.madcat.tmem.backend.BackendRequestor;
import edu.isi.madcat.tmem.backend.messages.AuxiliaryRule;
import edu.isi.madcat.tmem.backend.messages.BiasedTranslation;
import edu.isi.madcat.tmem.backend.messages.TranslationHypothesis;
import edu.isi.madcat.tmem.backend.messages.TranslationRequest;
import edu.isi.madcat.tmem.backend.messages.TranslationResponse;
import edu.isi.madcat.tmem.backend.messages.WordAlignment;
import edu.isi.madcat.tmem.exceptions.CateProcessException;
import edu.isi.madcat.tmem.lextrans.LexTranslator;
import edu.isi.madcat.tmem.lextrans.LexTranslatorConfig;
import edu.isi.madcat.tmem.logging.ExceptionHandler;
import edu.isi.madcat.tmem.processors.HierarchicalProcessor;
import edu.isi.madcat.tmem.processors.SubstringProcessor;
import edu.isi.madcat.tmem.sql.SqlManager;
import edu.isi.madcat.tmem.tokenize.Tokenizer;
import edu.isi.madcat.tmem.tokenize.TokenizerFactory;
import edu.isi.madcat.tmem.translation.AltPhrase;
import edu.isi.madcat.tmem.translation.DisplayPhrase;
import edu.isi.madcat.tmem.translation.RawTranslationResult;
import edu.isi.madcat.tmem.translation.SourceSegment;
import edu.isi.madcat.tmem.translation.TranslatedSegment;
import edu.isi.madcat.tmem.translation.TranslationHelper;
import edu.isi.madcat.tmem.translation.UserTranslationInfo;
import edu.isi.madcat.tmem.utils.Utils;

public class TranslationManager {
  public BackendRequestor getBackendRequestor() {
    return backendRequestor;
  }

  public void setBackendRequestor(BackendRequestor backendRequestor) {
    this.backendRequestor = backendRequestor;
  }

  public TranslationConfig getTranslationConfig() {
    return translationConfig;
  }

  public void setTranslationConfig(TranslationConfig translationConfig) {
    this.translationConfig = translationConfig;
  }

  public TranslationHelper getTranslationHelper() {
    return translationHelper;
  }

  public void setTranslationHelper(TranslationHelper translationHelper) {
    this.translationHelper = translationHelper;
  }

  public int getNextId() {
    return nextId;
  }

  public void setNextId(int nextId) {
    this.nextId = nextId;
  }

  private BackendRequestor backendRequestor;
  private TranslationConfig translationConfig;
  private TranslationHelper translationHelper;
  private Tokenizer sourceTokenizer;
  private int nextId;

  public TranslationManager(BeanFactory factory) {
    backendRequestor = (BackendRequestor) Utils.getRequiredBean(factory, "backendRequestor");

    translationConfig = (TranslationConfig) Utils.getRequiredBean(factory, "translationConfig");

    sourceTokenizer = null;
    if (translationConfig.getSourceTokenizerName() != null) {
      sourceTokenizer =
          TokenizerFactory.create(translationConfig.getSourceTokenizerName(), translationConfig
              .getSourceTokenizerConfig());
    }

    translationHelper =
        new TranslationHelper(translationConfig.getSourceLanguage(), translationConfig
            .getTargetLanguage(), backendRequestor);

    if (factory.containsBean("substringProcessor")) {
      SubstringProcessor substringProcessor =
          (SubstringProcessor) Utils.getRequiredBean(factory, "substringProcessor");
      substringProcessor.setId("sp");
      translationHelper.setSubstringProcessor(substringProcessor);
    }

    if (factory.containsBean("tokenProcessor")) {
      SubstringProcessor tokenProcessor =
          (SubstringProcessor) Utils.getRequiredBean(factory, "tokenProcessor");
      tokenProcessor.setId("tp");
      translationHelper.setTokenProcessor(tokenProcessor);
    }

    if (factory.containsBean("hierarchicalProcessor")) {
      HierarchicalProcessor hierarchicalProcessor =
          (HierarchicalProcessor) Utils.getRequiredBean(factory, "hierarchicalProcessor");
      translationHelper.setHierarchicalProcessor(hierarchicalProcessor);
    }

    if (factory.containsBean("lexTranslatorConfig")) {
      LexTranslatorConfig lexTranslatorConfig =
          (LexTranslatorConfig) Utils.getRequiredBean(factory, "lexTranslatorConfig");
      LexTranslator lexTranslator = new LexTranslator(lexTranslatorConfig);
      translationHelper.setLexTranslator(lexTranslator);
    }

    if (sourceTokenizer != null) {
      translationHelper.setSourceTokenizer(sourceTokenizer);
    }
    if (translationConfig.getSqlConfigFile() != null) {
      SqlManager sqlManager = new SqlManager(translationConfig.getSqlConfigFile());
      translationHelper.setSqlManager(sqlManager);
    }
    nextId = 0;
  }

  public static TranslationManager fromXmlFile(String configFile) {
    TranslationManager manager = null;
    try {
      FileInputStream inputStream = new FileInputStream(configFile);
      manager = TranslationManager.fromInputStream(inputStream);
      inputStream.close();
    } catch (FileNotFoundException e) {
      ExceptionHandler.handle(e);
    } catch (IOException e) {
      ExceptionHandler.handle(e);
    }

    return manager;
  }

  public static TranslationManager fromInputStream(InputStream inputStream) {
    DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
    reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);
    reader.loadBeanDefinitions(new InputStreamResource(inputStream));
    return new TranslationManager(factory);
  }

  public TranslatedSegment translate(String inputString, UserTranslationInfo userInfo)
      throws CateProcessException {
    SourceSegment segment = new SourceSegment(nextId, inputString);
    nextId++;
    TranslatedSegment translatedSegment = translationHelper.translate(segment, userInfo);
    if (translatedSegment != null) {
      translatedSegment.createDisplayPhrases();
    }
    return translatedSegment;
  }

  public RawTranslationResult translateWithRawResults(String inputString, int nbestSize)
      throws CateProcessException {
    SourceSegment segment = new SourceSegment(nextId, inputString);
    nextId++;
    RawTranslationResult result = translationHelper.translateWithRawResults(segment, nbestSize);
    return result;
  }

  public TranslationResponse translatePhrase(List<String> sourceTokens, int nbestSize)
      throws CateProcessException {
    List<AuxiliaryRule> auxiliaryRules = new ArrayList<AuxiliaryRule>();
    List<BiasedTranslation> forcedTranslations = new ArrayList<BiasedTranslation>();
    TranslationRequest translationRequest =
        new TranslationRequest(sourceTokens, auxiliaryRules, forcedTranslations, new Boolean(true),
            new Long(nbestSize));
    TranslationResponse translationResponse = backendRequestor.translate(translationRequest);
    return translationResponse;
  }

  public void getAltPhrases(TranslatedSegment segment) throws CateProcessException {
    if (segment == null) {
      return;
    }
    Map<Integer, List<Integer>> targetToTokSource = segment.createTargetToTokSourceMap();
    for (int i = 0; i < segment.getTargetWords().size(); i++) {
      List<Integer> srcIndexes = targetToTokSource.get(i);
      if (srcIndexes == null) {
        continue;
      }
      Integer phraseIndex = segment.getTargetWordToPhrase().get(i);
      if (phraseIndex == null) {
        continue;
      }
      DisplayPhrase currentPhrase = segment.getPhrases().get(phraseIndex);
      List<String> sourceTokens = new ArrayList<String>();
      for (int index : srcIndexes) {
        sourceTokens.add(segment.getSourceWords().get(index));
      }
      TranslationResponse response =
          translatePhrase(sourceTokens, translationConfig.getRequestAltNbestSize());
      List<AltPhrase> altPhrases = new ArrayList<AltPhrase>();
      Map<String, Integer> phraseCounts = new HashMap<String, Integer>();
      Set<String> seenTranslations = new HashSet<String>();
      for (TranslationHypothesis hyp : response.getHypotheses()) {
        if (altPhrases.size() >= translationConfig.getPrintAltNbestSize()) {
          break;
        }
        List<String> filteredWords = getFilteredPhrase(hyp);
        String targetPhrase = StringUtils.join(filteredWords, " ");
        if (seenTranslations.contains(targetPhrase)) {
          continue;
        }
        String pruningPhrase = getPruningPhrase(filteredWords);
        int phraseCount = 0;
        if (phraseCounts.containsKey(pruningPhrase)) {
          phraseCount = phraseCounts.get(pruningPhrase);
        }
        phraseCount++;
        if (phraseCount <= translationConfig.getMaxPhraseDupe()) {
          seenTranslations.add(targetPhrase);
          AltPhrase altPhrase = new AltPhrase(targetPhrase);
          altPhrases.add(altPhrase);
          phraseCounts.put(pruningPhrase, phraseCount);
        }
      }
      currentPhrase.setAltPhrases(altPhrases);
    }
  }

  private List<String> getFilteredPhrase(TranslationHypothesis hyp) {
    List<String> words = hyp.getTranslatedWords();
    int firstIndex = -1;
    int lastIndex = -1;
    for (WordAlignment align : hyp.getWordAlignment()) {
      int sourceIndex = align.getSourceIndex().intValue();
      int targetIndex = align.getTargetIndex().intValue();
      if (sourceIndex == -1 || targetIndex == -1) {
        continue;
      }
      if (firstIndex == -1 || targetIndex < firstIndex) {
        firstIndex = targetIndex;
      }
      if (lastIndex == -1 || targetIndex > lastIndex) {
        lastIndex = targetIndex;
      }
    }

    if (firstIndex == -1) {
      firstIndex = 0;
    }
    if (lastIndex == -1) {
      lastIndex = words.size() - 1;
    }
    List<String> filteredWords = new ArrayList<String>();
    for (int i = firstIndex; i <= lastIndex; i++) {
      filteredWords.add(words.get(i));
    }
    return filteredWords;
  }

  private String getPruningPhrase(List<String> words) {
    List<String> filteredWords = new ArrayList<String>();
    for (String word : words) {
      if (translationConfig.getTargetFunctionWords() == null
          || !translationConfig.getTargetFunctionWords().contains(word)) {
        filteredWords.add(word);
      }
    }
    if (filteredWords.size() == 0) {
      filteredWords = words;
    }
    return StringUtils.join(filteredWords, " ");
  }
}
