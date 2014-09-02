package edu.isi.madcat.tmem.training.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import edu.isi.madcat.tmem.alignment.TokenAlignment;
import edu.isi.madcat.tmem.logging.ExceptionHandler;
import edu.isi.madcat.tmem.tokenize.ReversableTokenizer;
import edu.isi.madcat.tmem.tokenize.TokenizerFactory;
import edu.isi.madcat.tmem.utils.ParameterMap;
import edu.isi.madcat.tmem.utils.Utils;

public class ReversableTokenizePlain {
  private ReversableTokenizer tokenizer;

  private String inputFile;
  private String outputFile;

  public ReversableTokenizePlain(ParameterMap params) {
    String tokenizerName = params.getStringRequired("tokenizer_name");
    String tokenizerParamFile = params.getStringRequired("tokenizer_param_file");
    inputFile = params.getStringRequired("input_file");
    outputFile = params.getStringRequired("output_file");

    tokenizer = TokenizerFactory.createReversable(tokenizerName, tokenizerParamFile);
  }

  void process() {
    Writer writer = Utils.createWriter(outputFile);
    BufferedReader reader = new BufferedReader(new InputStreamReader(Utils.openFile(inputFile)));
    String line = null;
    try {
      while ((line = reader.readLine()) != null) {
        TokenAlignment alignment = new TokenAlignment();
        List<Integer> splitAlignment = new ArrayList<Integer>();
        List<String> rawTokens = new ArrayList<String>();
        List<String> tokens = tokenizer.tokenize(line, alignment, rawTokens, splitAlignment);
        writer.write(StringUtils.join(tokens, " ")+"\n");
      }
    } catch (IOException e) {
      ExceptionHandler.handle(e);
    }
    IOUtils.closeQuietly(writer);
    IOUtils.closeQuietly(reader);
  }

  public static void main(String[] args) {
    if (args.length != 1) {
      throw new RuntimeException("Usage: [app] parameter_file");
    }

    String parameterFile = args[0];
    ParameterMap params = new ParameterMap(parameterFile);
    ReversableTokenizePlain processor = new ReversableTokenizePlain(params);
    processor.process();
  }

}
