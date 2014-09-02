package edu.isi.madcat.tmem.tools;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import edu.isi.madcat.tmem.alignment.TokenAlignment;
import edu.isi.madcat.tmem.exceptions.CateProcessException;
import edu.isi.madcat.tmem.logging.ExceptionHandler;
import edu.isi.madcat.tmem.tokenize.Tokenizer;
import edu.isi.madcat.tmem.tokenize.TokenizerFactory;
import edu.isi.madcat.tmem.utils.CateInitializer;
import edu.isi.madcat.tmem.utils.ParameterMap;
import edu.isi.madcat.tmem.utils.TextSegment;
import edu.isi.madcat.tmem.utils.TextSegmentIterator;
import edu.isi.madcat.tmem.utils.Utils;

public class Tokenize {
  private Tokenizer tokenizer;
  private String tokenizerName;
  private String tokenizerParamFile;
  private String[] inputFiles;
  private String inputField;
  private String outputFile;
  private String outputField;
  private String outputAlignmentField;
  
  public Tokenize(ParameterMap params) {
    tokenizerName = params.getStringRequired("tokenizer_name");
    tokenizerParamFile = params.getString("tokenizer_param_file");
    inputFiles = params.getStringArrayRequired("input_file");
    inputField = params.getStringRequired("input_field");
    outputFile = params.getStringRequired("output_file");
    outputField = params.getStringRequired("output_field");
    outputAlignmentField = params.getStringRequired("output_alignment_field");

    tokenizer = TokenizerFactory.create(tokenizerName, tokenizerParamFile);
  }
  
  void process() {
    TextSegmentIterator segIt = new TextSegmentIterator(inputFiles);

    try {
      Writer writer = Utils.createWriter(outputFile);
      TextSegment segment = null;
      while ((segment = segIt.next()) != null) {
        String inputString = segment.getRequired(inputField);
        TokenAlignment alignment = new TokenAlignment();
        List<String> tokens = tokenizer.tokenize(inputString, alignment);
        segment.insert(outputField, StringUtils.join(tokens, " "));
        segment.insert(outputAlignmentField, alignment.toSimpleString());
        segment.write(writer);
      }
      writer.close();
    } catch (IOException e) {
      ExceptionHandler.handle(e);
    }
  }
  
  public static void main(String[] args) throws CateProcessException {
    CateInitializer.initialize();
    
    if (args.length != 1) {
      throw new RuntimeException("Usage: [app] parameter_file");
    }

    String parameterFile = args[0];
    ParameterMap params = new ParameterMap(parameterFile);
    Tokenize tokenize = new Tokenize(params);
    tokenize.process();
  }
}
