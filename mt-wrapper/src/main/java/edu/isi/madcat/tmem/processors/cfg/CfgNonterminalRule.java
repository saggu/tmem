package edu.isi.madcat.tmem.processors.cfg;

import java.util.List;

public class CfgNonterminalRule {

  private String label;

  private List<String> rhsLabels;

  private boolean isFullRule;

  public CfgNonterminalRule(String label, List<String> rhsLabels, boolean isFullRule) {
    super();
    this.label = label;
    this.rhsLabels = rhsLabels;
    this.isFullRule = isFullRule;
  }

  public String getLabel() {
    return label;
  }

  public List<String> getRhsLabels() {
    return rhsLabels;
  }

  public boolean isFullRule() {
    return isFullRule;
  }

  public void setFullRule(boolean isFullRule) {
    this.isFullRule = isFullRule;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public void setRhsLabels(List<String> rhsLabels) {
    this.rhsLabels = rhsLabels;
  }

  @Override
  public String toString() {
    return "CfgNonterminalRule [label=" + label + ", rhsLabels=" + rhsLabels + ", isFullRule="
        + isFullRule + "]";
  }

}
