package edu.isi.madcat.tmem.processors.cfg;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import edu.isi.madcat.tmem.utils.Utils;

public class CfgRuleSet {
  public static CfgRuleSet fromLines(int id, List<String> rawLines) {
    List<String> lines = new ArrayList<String>();
    for (String line : rawLines) {
      line = StringUtils.trim(line);
      if (line.length() > 0) {
        lines.add(line);
      }
    }

    String ruleSetSpec = lines.get(2);
    if (lines.size() < 4) {
      throw new RuntimeException("Number of lines must be >= 3");
    }

    Pattern namePattern = Pattern.compile("^\\*+\\s+(.*)\\s+\\*+$");
    Pattern ntRulePattern = Pattern.compile("^(\\S+?)\\s*=>\\s*(.*?)$");
    Pattern termRulePattern = Pattern.compile("^(\\S+)\\s*:\\s*(.*)$");
    Matcher nameMatcher = namePattern.matcher(lines.get(0));
    if (!nameMatcher.find()) {
      throw new RuntimeException();
    }
    CfgRuleSet rs = new CfgRuleSet();
    rs.id = id;
    rs.name = nameMatcher.group(1);
    for (int i = 3; i < lines.size(); i++) {
      String line = lines.get(i);
      Matcher m = null;
      m = ntRulePattern.matcher(line);
      boolean lineIsValid = false;
      if (m.find()) {
        String label = m.group(1);
        String rhsLabelString = m.group(2);
        List<String> rhsLabels = Utils.trimAndSplit(rhsLabelString);
        boolean isFullRule = false;
        if (label.equals("S")) {
          isFullRule = true;
        }
        CfgNonterminalRule rule = new CfgNonterminalRule(label, rhsLabels, isFullRule);
        rs.ntRules.add(rule);
        lineIsValid = true;
      } else {
        m = termRulePattern.matcher(line);
        if (m.find()) {
          CfgTerminalRule rule = CfgTerminalRule.fromString(line);
          if (rule != null) {
            rs.termRules.add(rule);
            lineIsValid = true;
          }
        }
      }
      if (!lineIsValid) {
        throw new RuntimeException("Malformed line: " + line);
      }
    }
    rs.parser = CfgParserFactory.create("brute_force", rs);
    rs.isCoveringRuleSet = ruleSetSpec.equals("C") ? true : false;
    rs.transformer = CfgTransformer.fromString(lines.get(2));
    return rs;
  }

  public static CfgRuleSet fromString(int id, String str) {
    String[] rawLines = StringUtils.split(str, "\n");
    List<String> lines = new ArrayList<String>();
    return CfgRuleSet.fromLines(id, lines);
  }

  private int id;

  private String name;

  private List<CfgNonterminalRule> ntRules;

  private List<CfgTerminalRule> termRules;

  private CfgParser parser;

  private CfgTransformer transformer;

  private boolean isCoveringRuleSet;

  public CfgRuleSet() {
    super();
    this.id = -1;
    this.name = null;
    this.ntRules = new ArrayList<CfgNonterminalRule>();
    this.termRules = new ArrayList<CfgTerminalRule>();
    this.parser = null;
    this.transformer = null;
    this.isCoveringRuleSet = false;
  }

  public int getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public List<CfgNonterminalRule> getNtRules() {
    return ntRules;
  }

  public CfgParser getParser() {
    return parser;
  }

  public List<CfgTerminalRule> getTermRules() {
    return termRules;
  }

  public CfgTransformer getTransformer() {
    return transformer;
  }

  public boolean isCoveringRuleSet() {
    return isCoveringRuleSet;
  }

  public CfgParserOutput processTokens(List<CfgInputToken> tokens) {
    CfgParserOutput parserOutput = parser.parseTokens(tokens);
    return parserOutput;
  }

  public void setCoveringRuleSet(boolean isCoveringRuleSet) {
    this.isCoveringRuleSet = isCoveringRuleSet;
  }

  public void setId(int id) {
    this.id = id;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setNtRules(List<CfgNonterminalRule> ntRules) {
    this.ntRules = ntRules;
  }

  public void setParser(CfgParser parser) {
    this.parser = parser;
  }

  public void setTermRules(List<CfgTerminalRule> termRules) {
    this.termRules = termRules;
  }

  public void setTransformer(CfgTransformer transformer) {
    this.transformer = transformer;
  }

}
