package edu.isi.madcat.tmem.processors.cfg;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import edu.isi.madcat.tmem.logging.ExceptionHandler;

public class CfgProcessor {
  public static CfgProcessor fromFile(String cfgFile) {
    List<String> lines = null;
    try {
      lines = FileUtils.readLines(new File(cfgFile), "utf-8");
    } catch (IOException e) {
      ExceptionHandler.handle(e);
    }

    Pattern namePattern = Pattern.compile("^\\*+\\s+(.*)\\s+\\*+$");
    List<List<String>> allRuleSetLines = new ArrayList<List<String>>();
    for (String line : lines) {
      line = StringUtils.trim(line);
      Matcher m = namePattern.matcher(line);
      if (m.find()) {
        List<String> setLines = new ArrayList<String>();
        allRuleSetLines.add(setLines);
      }
      List<String> setLines = allRuleSetLines.get(allRuleSetLines.size() - 1);
      setLines.add(line);
    }
    CfgProcessor processor = new CfgProcessor();
    int ruleSetId = 0;
    for (List<String> ruleSetLines : allRuleSetLines) {
      CfgRuleSet ruleSet = CfgRuleSet.fromLines(ruleSetId, ruleSetLines);
      if (ruleSet.isCoveringRuleSet()) {
        processor.coveringRuleSets.add(ruleSet);
      } else {
        processor.otherRuleSets.add(ruleSet);
      }
      ruleSetId++;
    }
    return processor;
  }

  private List<CfgRuleSet> coveringRuleSets;
  private List<CfgRuleSet> otherRuleSets;

  public CfgProcessor() {
    coveringRuleSets = new ArrayList<CfgRuleSet>();
    otherRuleSets = new ArrayList<CfgRuleSet>();
  }

  public CfgProcessorOutput processTokens(List<CfgInputToken> inputTokens) {

    List<CfgParserOutput> parserOutputs = new ArrayList<CfgParserOutput>();
    return null;
  }

  private CfgProcessorOutput processTokensRecursive(List<CfgInputToken> origInputTokens, int start,
      int end) {
    List<CfgInputToken> inputTokens = new ArrayList<CfgInputToken>();
    for (int j = start; j <= end; j++) {
      inputTokens.add(origInputTokens.get(j));
    }

    CfgProcessorOutput output = new CfgProcessorOutput(start, end);
    CfgParserOutput coveredOutput = null;
    for (int i = 0; i < coveringRuleSets.size(); i++) {
      CfgRuleSet ruleSet = coveringRuleSets.get(i);
      CfgParserOutput parserOutput = ruleSet.processTokens(inputTokens);
      parserOutput.setOffset(start);
      if (parserOutput.getRootNodes().size() > 1) {
        coveredOutput = parserOutput;
        break;
      }
    }

    if (coveredOutput != null) {
      CfgParserNode parserNode = coveredOutput.getRootNodes().get(0);
      List<CfgParserNode> ntRanges = parserNode.getNtRanges();
      List<CfgProcessorOutput> childOutputs = new ArrayList<CfgProcessorOutput>();
      for (int i = 0; i < ntRanges.size(); i++) {
        CfgParserNode ntRange = ntRanges.get(i);
        CfgProcessorOutput childOutput =
            processTokensRecursive(origInputTokens, ntRange.getStart(), ntRange.getEnd());
        childOutputs.add(childOutput);
      }
      output.setCoveringChildren(childOutputs);
    } else {
      List<CfgParserOutput> otherParserOutputs = new ArrayList<CfgParserOutput>();
      for (int i = 0; i < otherRuleSets.size(); i++) {
        CfgRuleSet ruleSet = otherRuleSets.get(i);
        CfgParserOutput parserOutput = ruleSet.processTokens(inputTokens);
        parserOutput.setOffset(start);
        otherParserOutputs.add(parserOutput);
      }
      output.setOtherChildren(otherParserOutputs);
    }
    return output;
  }

}
