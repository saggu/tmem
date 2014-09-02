package edu.isi.madcat.tmem.processors.cfg;

import java.util.List;

public class CfgParserOutput {

  private CfgRuleSet ruleSet;

  private List<CfgParserNode> rootNodes;

  public CfgParserOutput(CfgRuleSet ruleSet, List<CfgParserNode> rootNodes) {
    super();
    this.ruleSet = ruleSet;
    this.rootNodes = rootNodes;
  }

  public List<CfgParserNode> getRootNodes() {
    return rootNodes;
  }

  public CfgRuleSet getRuleSet() {
    return ruleSet;
  }

  public void setOffset(int start) {
    for (CfgParserNode node : rootNodes) {
      node.setOffset(start);
    }
  }

  public void setRootNodes(List<CfgParserNode> rootNodes) {
    this.rootNodes = rootNodes;
  }

  public void setRuleSet(CfgRuleSet ruleSet) {
    this.ruleSet = ruleSet;
  }

  @Override
  public String toString() {
    return "CfgParserOutput [ruleSet=" + ruleSet + ", rootNodes=" + rootNodes + "]";
  }

}
