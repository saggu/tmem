package edu.isi.madcat.tmem.processors.cfg;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CfgTerminalRule {
  private enum MatchType {
    EXACT_MATCH, REGEX
  }

  public static CfgTerminalRule fromString(String str) {
    Pattern termRulePattern = Pattern.compile("^(\\S+)\\s*:\\s*(.*)$");
    Pattern isRegexPattern = Pattern.compile("^/(.*)/$");
    Matcher m = termRulePattern.matcher(str);
    if (!m.find()) {
      return null;
    }
    String label = m.group(1);
    String patternString = m.group(2);
    CfgTerminalRule rule = new CfgTerminalRule();
    rule.label = label;
    rule.matchType = MatchType.EXACT_MATCH;
    rule.matchString = patternString;
    rule.pattern = null;
    m = isRegexPattern.matcher(patternString);
    if (m.find()) {
      rule.matchType = MatchType.REGEX;
      rule.matchString = m.group(1);
      rule.pattern = Pattern.compile(rule.matchString);
    }
    return rule;
  }

  private String label;

  private MatchType matchType;

  private String matchString;

  private Pattern pattern;

  public CfgTerminalRule() {
  }

  public String getLabel() {
    return label;
  }

  public String getMatchString() {
    return matchString;
  }

  public MatchType getMatchType() {
    return matchType;
  }

  public Pattern getPattern() {
    return pattern;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public void setMatchString(String matchString) {
    this.matchString = matchString;
  }

  public void setMatchType(MatchType matchType) {
    this.matchType = matchType;
  }

  public void setPattern(Pattern pattern) {
    this.pattern = pattern;
  }

  CfgTerminalOutput getTerminalOutput(CfgInputToken inputToken) {
    if (matchType == MatchType.EXACT_MATCH) {
      if (inputToken.getType() == CfgInputToken.TokenType.SOS && matchString.equals("<s>")) {
        return new CfgTerminalOutput(inputToken.getText());
      } else if (inputToken.getType() == CfgInputToken.TokenType.EOS && matchString.equals("</s>")) {
        return new CfgTerminalOutput(inputToken.getText());
      } else if (inputToken.getText().equals(matchString)) {
        return new CfgTerminalOutput(inputToken.getText());
      }
    } else if (matchType == MatchType.REGEX) {
      Matcher m = pattern.matcher(inputToken.getText());
      if (m.find()) {
        List<String> groups = new ArrayList<String>();
        for (int i = 0; i <= m.groupCount(); i++) {
          groups.add(m.group(i));
        }
        return new CfgTerminalOutput(groups);
      }
    }
    return null;
  }
}
