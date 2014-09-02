package edu.isi.madcat.tmem.training.tools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import edu.isi.madcat.tmem.logging.ExceptionHandler;
import edu.isi.madcat.tmem.processors.cfg.CfgInputToken;
import edu.isi.madcat.tmem.processors.cfg.CfgProcessor;
import edu.isi.madcat.tmem.utils.ParameterMap;

public class RunCfgParsing {
  private CfgProcessor processor;
  private List<String> inputStrings;

  public RunCfgParsing(ParameterMap params) {
    String[] inputFiles = params.getStringArrayRequired("input_files");
    String cfgFile = params.getStringRequired("cfg_file");

    processor = CfgProcessor.fromFile(cfgFile);

    inputStrings = new ArrayList<String>();
    for (String inputFile : inputFiles) {
      try {
        List<String> lines = FileUtils.readLines(new File(inputFile), "utf-8");
        inputStrings.addAll(lines);
      } catch (IOException e) {
        ExceptionHandler.handle(e);
      }
    }
  }

  public void process() {
    System.out.println(processor.toString());
    for (String inputString : inputStrings) {
      String[] array = StringUtils.split(inputString, " ");
      List<CfgInputToken> inputTokens = new ArrayList<CfgInputToken>();
//      inputTokens.add(CfgInputToken.createStartToken());
      for (String item : array) {
        inputTokens.add(new CfgInputToken(CfgInputToken.TokenType.NORMAL, item));
      }
//      inputTokens.add(CfgInputToken.createEndToken());
      processor.processTokens(inputTokens);
    }
  }

  public static void main(String[] args) {
    ParameterMap params = ParameterMap.readFromArgs(args);
    RunCfgParsing segmentor = new RunCfgParsing(params);
    segmentor.process();
  }
}
