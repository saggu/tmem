package edu.isi.madcat.tmem.processors.cfg;

public class CfgParserFactory {
  public static CfgParser create(String type, CfgRuleSet ruleSet) {
    CfgParser parser = null;
    if (type.equals("brute_force")) {
      parser = new CfgBruteForceParser();
    }
    parser.initialize(ruleSet);
    return parser;
  }
}
