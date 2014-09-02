package edu.isi.madcat.tmem.lextrans.tools;

import java.util.ArrayList;
import java.util.List;

import edu.isi.madcat.tmem.alignment.TokenAlignment;
import edu.isi.madcat.tmem.backend.messages.AuxiliaryRule;
import edu.isi.madcat.tmem.backend.messages.BiasedTranslation;
import edu.isi.madcat.tmem.backend.messages.TranslationRequest;
import edu.isi.madcat.tmem.backend.messages.TranslationResponse;
import edu.isi.madcat.tmem.lextrans.LexTranslator;
import edu.isi.madcat.tmem.lextrans.LexTranslatorConfig;
import edu.isi.madcat.tmem.tokenize.Tokenizer;
import edu.isi.madcat.tmem.tokenize.TokenizerFactory;
import edu.isi.madcat.tmem.utils.ParameterMap;
import edu.isi.madcat.tmem.utils.TextSegment;
import edu.isi.madcat.tmem.utils.TextSegmentIterator;

public class RunTranslator {
  public static void main(String[] args) {
    if (args.length != 1) {
      throw new RuntimeException("Usage: [app] parameter_file");
    }

    String parameterFile = args[0];
    ParameterMap params = new ParameterMap(parameterFile);
    String tokenizerName = params.getStringRequired("tokenizer_name");
    String tokenizerParamFile = params.getString("tokenizer_param_file");
    String modelFile = params.getStringRequired("model_file");
    String inputFile = params.getStringRequired("input_file");
    
    System.out.println("Creating tokenizer");
    Tokenizer tokenizer = TokenizerFactory.create(tokenizerName, tokenizerParamFile);
    System.out.println("Done");
    
    System.out.println("Creating translation model");
    LexTranslatorConfig config = new LexTranslatorConfig();
    config.setModelFile(modelFile);
    LexTranslator translator = new LexTranslator(config);
    System.out.println("Done");
    TextSegmentIterator segIt = new TextSegmentIterator(inputFile);
    TextSegment segment = null;
    while ((segment = segIt.next()) != null) {
      String source = segment.getRequired("SOURCE");
      TokenAlignment alignment = new TokenAlignment();
      List<String> sourceWords = tokenizer.tokenize(source, alignment);
      TranslationRequest request = new TranslationRequest(sourceWords, new ArrayList<AuxiliaryRule>(), new ArrayList<BiasedTranslation>(), new Boolean(false), new Long(1));
      TranslationResponse response = translator.translate(request);
      System.out.println(response);
    }
  }
}
