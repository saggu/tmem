package edu.isi.madcat.tmem.training.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import edu.isi.madcat.tmem.alignment.TokenAlignment;
import edu.isi.madcat.tmem.logging.ExceptionHandler;
import edu.isi.madcat.tmem.tokenize.Tokenizer;
import edu.isi.madcat.tmem.tokenize.TokenizerFactory;
import edu.isi.madcat.tmem.utils.ParameterMap;
import edu.isi.madcat.tmem.utils.Utils;

public class TokenizePlain {
  private Tokenizer tokenizer;

  private String inputFile;
  private String outputFile;

  public TokenizePlain(ParameterMap params) {
    String tokenizerName = params.getStringRequired("tokenizer_name");
    String tokenizerParamFile = params.getString("tokenizer_param_file");
    inputFile = params.getStringRequired("input_file");
    outputFile = params.getStringRequired("output_file");

    tokenizer = TokenizerFactory.create(tokenizerName, tokenizerParamFile);
  }

  void process() {
    Writer writer = Utils.createWriter(outputFile);
    BufferedReader reader = new BufferedReader(new InputStreamReader(Utils.openFile(inputFile)));
    String line = null;
    try {
      while ((line = reader.readLine()) != null) {
        TokenAlignment alignment = new TokenAlignment();
        List<String> tokens = tokenizer.tokenize(line, alignment);
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
    TokenizePlain processor = new TokenizePlain(params);
    processor.process();
  }

}
