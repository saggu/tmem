package edu.isi.madcat.tmem.training.tools;

import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import edu.isi.madcat.tmem.alignment.TokenAlignment;
import edu.isi.madcat.tmem.tokenize.ReversableTokenizer;
import edu.isi.madcat.tmem.tokenize.TokenizerFactory;
import edu.isi.madcat.tmem.utils.ParameterMap;
import edu.isi.madcat.tmem.utils.TextSegment;
import edu.isi.madcat.tmem.utils.TextSegmentIterator;
import edu.isi.madcat.tmem.utils.Utils;

public class ReversableTokenize {
  private ReversableTokenizer tokenizer;
  private String[] inputFiles;
  private String inputField;
  private String rawTokenField;
  private String tokenizedField;
  private String splitAlignField;
  private String outputFile;

  public ReversableTokenize(ParameterMap params) {
    String tokenizerName = params.getStringRequired("tokenizer_name");
    String tokenizerParamFile = params.getStringRequired("tokenizer_param_file");
    inputFiles = params.getStringArrayRequired("input_files");
    inputField = params.getStringRequired("input_field");
    rawTokenField = params.getStringRequired("raw_token_field");
    tokenizedField = params.getStringRequired("tokenized_field");
    splitAlignField = params.getStringRequired("split_align_field");
    outputFile = params.getStringRequired("output_file");

    tokenizer = TokenizerFactory.createReversable(tokenizerName, tokenizerParamFile);
  }

  void process() {
    TextSegmentIterator segIt = new TextSegmentIterator(inputFiles);
    TextSegment segment = null;
    Writer writer = Utils.createWriter(outputFile);
    while ((segment = segIt.next()) != null) {
      String rawText = segment.getRequired(inputField);
      TokenAlignment alignment = new TokenAlignment();
      List<Integer> splitAlignment = new ArrayList<Integer>();
      List<String> rawTokens = new ArrayList<String>();
      List<String> tokens = tokenizer.tokenize(rawText, alignment, rawTokens, splitAlignment);
      segment.insert(tokenizedField, StringUtils.join(tokens, " "));
      segment.insert(rawTokenField, StringUtils.join(rawTokens, " "));
      segment.insert(splitAlignField, StringUtils.join(splitAlignment, " "));
      segment.write(writer);
    }
    segIt.close();
    IOUtils.closeQuietly(writer);
  }

  public static void main(String[] args) {
    if (args.length != 1) {
      throw new RuntimeException("Usage: [app] parameter_file");
    }

    String parameterFile = args[0];
    ParameterMap params = new ParameterMap(parameterFile);
    ReversableTokenize processor = new ReversableTokenize(params);
    processor.process();
  }
}
