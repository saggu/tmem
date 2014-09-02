package edu.isi.madcat.tmem.processors.cfg;

import java.util.ArrayList;
import java.util.List;

public class CfgTerminalOutput {
  private List<String> groups;

  private int ruleIndex;

  public CfgTerminalOutput(List<String> groups) {
    this.groups = groups;
  }

  public CfgTerminalOutput(String text) {
    groups = new ArrayList<String>();
    groups.add(text);
  }

  public List<String> getGroups() {
    return groups;
  }

  public int getRuleIndex() {
    return ruleIndex;
  }

  public void setGroups(List<String> groups) {
    this.groups = groups;
  }

  public void setRuleIndex(int ruleIndex) {
    this.ruleIndex = ruleIndex;
  }

  @Override
  public String toString() {
    return "CfgTerminalOutput [groups=" + groups + ", ruleIndex=" + ruleIndex + "]";
  }

}
