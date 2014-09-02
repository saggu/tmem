package edu.isi.madcat.tmem.processors.cfg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class CfgBruteForceParser extends CfgParser {
  public class ParsedRule {
    private Integer ntRuleIndex;

    private Integer termRuleIndex;

    private String label;

    private SubRange range;

    private List<SubRange> ranges;

    public ParsedRule(Integer ntRuleIndex, Integer termRuleIndex, String label, SubRange range,
        List<SubRange> ranges) {
      super();
      this.ntRuleIndex = ntRuleIndex;
      this.termRuleIndex = termRuleIndex;
      this.label = label;
      this.range = range;
      this.ranges = ranges;
    }

    public String getLabel() {
      return label;
    }

    public Integer getNtRuleIndex() {
      return ntRuleIndex;
    }

    public SubRange getRange() {
      return range;
    }

    public List<SubRange> getRanges() {
      return ranges;
    }

    public Integer getTermRuleIndex() {
      return termRuleIndex;
    }

    public void setLabel(String label) {
      this.label = label;
    }

    public void setNtRuleIndex(Integer ntRuleIndex) {
      this.ntRuleIndex = ntRuleIndex;
    }

    public void setRange(SubRange range) {
      this.range = range;
    }

    public void setRanges(List<SubRange> ranges) {
      this.ranges = ranges;
    }

    public void setTermRuleIndex(Integer termRuleIndex) {
      this.termRuleIndex = termRuleIndex;
    }

    @Override
    public String toString() {
      return "ParsedRule [ntRuleIndex=" + ntRuleIndex + ", termRuleIndex=" + termRuleIndex
          + ", label=" + label + ", range=" + range + ", ranges=" + ranges + "]";
    }

  }

  public class State {
    private List<CfgInputToken> tokens;

    private List<List<CfgTerminalOutput>> terminalOutputs;

    private List<List<List<ParsedRule>>> parsedRules;

    private Map<SubRange, List<ParsedRule>> fullRules;

    private IdentityHashMap<ParsedRule, CfgParserNode> parsedRuleToRuleMatch;

    public State(List<CfgInputToken> tokens) {
      super();
      this.tokens = tokens;
      this.terminalOutputs = new ArrayList<List<CfgTerminalOutput>>();
      this.parsedRules = new ArrayList<List<List<ParsedRule>>>();
      for (int i = 0; i < tokens.size(); i++) {
        List<List<ParsedRule>> parsedRuleList = new ArrayList<List<ParsedRule>>();
        this.parsedRules.add(parsedRuleList);
        for (int j = 0; j < tokens.size(); j++) {
          List<ParsedRule> parsedRules = new ArrayList<ParsedRule>();
          parsedRuleList.add(parsedRules);
        }
      }
      this.fullRules = new HashMap<SubRange, List<ParsedRule>>();
      this.parsedRuleToRuleMatch = new IdentityHashMap<ParsedRule, CfgParserNode>();
    }

    public Map<SubRange, List<ParsedRule>> getFullRules() {
      return fullRules;
    }

    public List<List<List<ParsedRule>>> getParsedRules() {
      return parsedRules;
    }

    public List<List<CfgTerminalOutput>> getTerminalOutputs() {
      return terminalOutputs;
    }

    public List<CfgInputToken> getTokens() {
      return tokens;
    }

    public void setFullRules(Map<SubRange, List<ParsedRule>> fullRules) {
      this.fullRules = fullRules;
    }

    public void setParsedRules(List<List<List<ParsedRule>>> parsedRules) {
      this.parsedRules = parsedRules;
    }

    public void setTerminalOutputs(List<List<CfgTerminalOutput>> terminalOutputs) {
      this.terminalOutputs = terminalOutputs;
    }

    public void setTokens(List<CfgInputToken> tokens) {
      this.tokens = tokens;
    }

    @Override
    public String toString() {
      return "State [tokens=" + tokens + ", terminalOutputs=" + terminalOutputs + ", parsedRules="
          + parsedRules + ", fullRules=" + fullRules + ", parsedRuleToRuleMatch="
          + parsedRuleToRuleMatch + "]";
    }

  }

  public class SubRange {
    public int start;

    public int end;

    public SubRange(int start, int end) {
      super();
      this.start = start;
      this.end = end;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      SubRange other = (SubRange) obj;
      if (!getOuterType().equals(other.getOuterType()))
        return false;
      if (end != other.end)
        return false;
      if (start != other.start)
        return false;
      return true;
    }

    public int getEnd() {
      return end;
    }

    public int getStart() {
      return start;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + getOuterType().hashCode();
      result = prime * result + end;
      result = prime * result + start;
      return result;
    }

    public void setEnd(int end) {
      this.end = end;
    }

    public void setStart(int start) {
      this.start = start;
    }

    @Override
    public String toString() {
      return "SubRange [start=" + start + ", end=" + end + "]";
    }

    private CfgBruteForceParser getOuterType() {
      return CfgBruteForceParser.this;
    }
  }

  private CfgRuleSet ruleSet;

  private List<CfgNonterminalRule> ntRules;

  private List<CfgTerminalRule> termRules;

  private Map<String, List<Integer>> labelToTermRules;

  private Map<String, List<Integer>> labelToNtRules;

  public ParsedRule createNtParse(Integer ntRuleIndex, String label, SubRange range,
      List<SubRange> ranges) {
    ParsedRule rule = new ParsedRule(ntRuleIndex, -1, label, range, ranges);
    return rule;
  }

  public ParsedRule createTermParse(Integer termRuleIndex, String label, SubRange range) {
    ParsedRule rule = new ParsedRule(-1, termRuleIndex, label, range, new ArrayList<SubRange>());
    return rule;
  }

  @Override
  public void initialize(CfgRuleSet ruleSet) {
    this.ruleSet = ruleSet;
    ntRules = ruleSet.getNtRules();
    termRules = ruleSet.getTermRules();

    labelToTermRules = new HashMap<String, List<Integer>>();
    for (int i = 0; i < termRules.size(); i++) {
      CfgTerminalRule termRule = termRules.get(i);
      String label = termRule.getLabel();
      List<Integer> termRuleIndexes = labelToTermRules.get(label);
      if (termRuleIndexes == null) {
        termRuleIndexes = new ArrayList<Integer>();
        labelToTermRules.put(label, termRuleIndexes);
      }
      termRuleIndexes.add(i);
    }

    labelToNtRules = new HashMap<String, List<Integer>>();
    for (int i = 0; i < ntRules.size(); i++) {
      CfgNonterminalRule ntRule = ntRules.get(i);
      String label = ntRule.getLabel();
      List<Integer> ntRuleIndexes = labelToTermRules.get(label);
      if (ntRuleIndexes == null) {
        ntRuleIndexes = new ArrayList<Integer>();
        labelToTermRules.put(label, ntRuleIndexes);
      }
      ntRuleIndexes.add(i);
    }
  }

  @Override
  public CfgParserOutput parseTokens(List<CfgInputToken> tokens) {
    State state = new State(tokens);

    for (int i = 0; i < tokens.size(); i++) {
      CfgInputToken inputToken = tokens.get(i);
      List<CfgTerminalOutput> currentOutput = new ArrayList<CfgTerminalOutput>();
      for (int j = 0; j < termRules.size(); j++) {
        CfgTerminalOutput termOutput = termRules.get(j).getTerminalOutput(inputToken);
        if (termOutput != null) {
          termOutput.setRuleIndex(j);
          currentOutput.add(termOutput);
          state.getParsedRules().get(i).get(i).add(
              createTermParse(j, termRules.get(j).getLabel(), new SubRange(i, i)));
        }
      }
      state.getTerminalOutputs().add(currentOutput);
    }

    for (int length = 1; length <= tokens.size(); length++) {
      for (int start = 0; start < tokens.size() - length + 1; start++) {
        int end = start + length - 1;
        parseSpan(state, start, end);
      }
    }

    IdentityHashMap<Object, Integer> seenItems = new IdentityHashMap<Object, Integer>();
    for (Map.Entry<SubRange, List<ParsedRule>> e : state.getFullRules().entrySet()) {
      List<ParsedRule> fullRules = e.getValue();
      for (ParsedRule rule : fullRules) {
        addNtRuleMatch(state, rule);
      }
    }

    List<CfgParserNode> rootNodes = new ArrayList<CfgParserNode>();
    for (Map.Entry<SubRange, List<ParsedRule>> e : state.getFullRules().entrySet()) {
      List<ParsedRule> fullRules = e.getValue();
      for (ParsedRule parsedRule : fullRules) {
        CfgParserNode parserNode = state.parsedRuleToRuleMatch.get(parsedRule);
        rootNodes.add(parserNode);
      }
    }
    CfgParserOutput output = new CfgParserOutput(ruleSet, rootNodes);
    return output;
  }

  private CfgParserNode addNtRuleMatch(State state, ParsedRule parsedRule) {
    CfgParserNode rm = state.parsedRuleToRuleMatch.get(parsedRule);
    if (rm == null) {
      if (parsedRule.getNtRuleIndex() != -1) {
        List<CfgParserNode> allNtNodes = new ArrayList<CfgParserNode>();
        for (int i = 0; i < parsedRule.getRanges().size(); i++) {
          CfgParserNode ntNode = null;
          int subStart = parsedRule.getRanges().get(i).getStart();
          int subEnd = parsedRule.getRanges().get(i).getEnd();
          CfgNonterminalRule ntRule = ntRules.get(parsedRule.getNtRuleIndex());
          String label = ntRule.getRhsLabels().get(i);
          List<ParsedRule> subParsedRules = state.getParsedRules().get(subStart).get(subEnd);
          for (int j = 0; j < subParsedRules.size(); j++) {
            if (label.equals(subParsedRules.get(j).getLabel())) {
              CfgParserNode childParserNode = addNtRuleMatch(state, subParsedRules.get(j));
              ntNode = childParserNode;
              break;
            }
          }
          allNtNodes.add(ntNode);
        }
        rm =
            new CfgParserNode(parsedRule.getNtRuleIndex(), -1, parsedRule.getRange().getStart(),
                parsedRule.getRange().getEnd(), allNtNodes);
      } else {
        rm =
            new CfgParserNode(-1, parsedRule.getTermRuleIndex(), parsedRule.getRange().getStart(),
                parsedRule.getRange().getEnd(), null);
      }
      state.parsedRuleToRuleMatch.put(parsedRule, rm);
    }
    return rm;
  }

  private void parseSpan(State state, int start, int end) {
    List<CfgInputToken> tokens = state.tokens;
    Map<String, List<SubRange>> labelToSubRange = new HashMap<String, List<SubRange>>();
    for (int i = start; i <= end; i++) {
      for (int j = i; j <= end; j++) {
        List<ParsedRule> parsedRules = state.parsedRules.get(i).get(j);
        for (ParsedRule rule : parsedRules) {
          String label = rule.label;
          List<SubRange> subRanges = labelToSubRange.get(label);
          if (subRanges == null) {
            subRanges = new ArrayList<SubRange>();
            labelToSubRange.put(label, subRanges);
          }
          subRanges.add(new SubRange(i, j));
        }
      }

    }
    for (int i = 0; i < ntRules.size(); i++) {
      CfgNonterminalRule ntRule = ntRules.get(i);
      boolean isValid = true;
      List<List<SubRange>> ntRanges = new ArrayList<List<SubRange>>();
      for (int j = 0; j < ntRule.getRhsLabels().size(); j++) {
        List<SubRange> curRanges = new ArrayList<SubRange>();
        String ntLabel = ntRule.getRhsLabels().get(j);
        List<SubRange> subRanges = labelToSubRange.get(ntLabel);
        if (subRanges != null) {
          for (SubRange subRange : subRanges) {
            if (j == 0 && subRange.start != start) {
              continue;
            }
            if (j == ntRule.getRhsLabels().size() - 1 && subRange.end != end) {
              continue;
            }
            curRanges.add(subRange);
          }
        }
        if (curRanges.size() == 0) {
          isValid = false;
        }
        ntRanges.add(curRanges);
      }
      if (!isValid) {
        continue;
      }
      Map<Integer, List<List<SubRange>>> endToRanges = new HashMap<Integer, List<List<SubRange>>>();
      for (int j = 0; j < ntRanges.size(); j++) {
        List<SubRange> subRanges = ntRanges.get(j);
        for (SubRange subRange : subRanges) {
          List<List<SubRange>> prevRangeSet = endToRanges.get(subRange.start - 1);
          if (prevRangeSet == null && j == 0) {
            prevRangeSet = new ArrayList<List<SubRange>>();
            prevRangeSet.add(new ArrayList<SubRange>());
            endToRanges.put(subRange.start - 1, prevRangeSet);
          }
          if (prevRangeSet != null) {
            for (List<SubRange> prevRanges : prevRangeSet) {
              List<SubRange> newRanges = new ArrayList<SubRange>(prevRanges);
              newRanges.add(subRange);
              List<List<SubRange>> nextRangeSet = endToRanges.get(subRange.end);
              if (nextRangeSet == null) {
                nextRangeSet = new ArrayList<List<SubRange>>();
                endToRanges.put(subRange.end, nextRangeSet);
              }
              nextRangeSet.add(newRanges);
            }
          }
        }
      }
      List<List<SubRange>> finalRangeSet = endToRanges.get(end);
      if (finalRangeSet != null) {
        for (List<SubRange> rangeSet : finalRangeSet) {
          ParsedRule parsedRule =
              createNtParse(i, ntRule.getLabel(), new SubRange(start, end), rangeSet);
          state.parsedRules.get(start).get(end).add(parsedRule);
          if (ntRule.isFullRule()) {
            if (!ruleSet.isCoveringRuleSet() || (start == 0 && end == state.tokens.size() - 1)) {
              SubRange subRange = new SubRange(start, end);
              List<ParsedRule> fullRules = state.getFullRules().get(subRange);
              if (fullRules == null) {
                fullRules = new ArrayList<ParsedRule>();
                state.getFullRules().put(subRange, fullRules);
              }
              fullRules.add(parsedRule);
            }
          }
        }
      }
    }
  }
}
