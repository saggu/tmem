package edu.isi.madcat.tmem.tools;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import edu.isi.madcat.tmem.backend.BackendRequestor;
import edu.isi.madcat.tmem.backend.messages.TranslationHypothesis;
import edu.isi.madcat.tmem.backend.messages.TranslationResponse;
import edu.isi.madcat.tmem.exceptions.CateProcessException;
import edu.isi.madcat.tmem.processors.SubstringProcessor;
import edu.isi.madcat.tmem.translation.TranslationHelper;
import edu.isi.madcat.tmem.utils.CateInitializer;
import edu.isi.madcat.tmem.utils.ParameterMap;
import edu.isi.madcat.tmem.utils.Utils;

public class PhraseTranslator {
  public static void main(String[] args) throws CateProcessException {
    CateInitializer.initialize();
    
    if (args.length != 1) {
      throw new RuntimeException("Usage: [app] parameter_file");
    }

    String parameterFile = args[0];
    ParameterMap params = new ParameterMap(parameterFile);
    String configFile = params.getStringRequired("config_file");
    String sourceLanguage = params.getStringRequired("source_language");
    String targetLanguage = params.getStringRequired("target_language");
    String inputFile = params.getStringRequired("input_file");
    int nbestSize = params.getIntRequired("nbest_size");

    FileSystemXmlApplicationContext appContext =
        new FileSystemXmlApplicationContext("file:" + configFile);

    BeanFactory factory = appContext;
    BackendRequestor backendRequestor =
        (BackendRequestor) Utils.getRequiredBean(factory, "backendRequestor");


    List<String> lines = Utils.readLinesFromFile(inputFile);
    SubstringProcessor substringProcessor = new SubstringProcessor();
    TranslationHelper translationHelper =
        new TranslationHelper(sourceLanguage, targetLanguage, backendRequestor);
    translationHelper.setSubstringProcessor(substringProcessor);
    
    for (String line : lines) {
      TranslationResponse translationResponse =
          translationHelper.translatePhrase(line, nbestSize);
      List<TranslationHypothesis> hypotheses = translationResponse.getHypotheses();
      for (TranslationHypothesis hypothesis : hypotheses) {
        System.out.println(StringUtils.join(hypothesis.getTranslatedWords(), " "));
      }
    } 
  }
}
