package edu.isi.madcat.tmem.processors.cfg;

import java.util.List;

public abstract class CfgParser {
  public abstract void initialize(CfgRuleSet ruleSet);

  public abstract CfgParserOutput parseTokens(List<CfgInputToken> tokens);
}
