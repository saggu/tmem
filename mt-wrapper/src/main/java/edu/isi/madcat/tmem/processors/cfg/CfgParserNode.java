package edu.isi.madcat.tmem.processors.cfg;

import java.util.List;

public class CfgParserNode {

  private int ntRuleIndex;

  private int termRuleIndex;

  private int start;

  private int end;
  private List<CfgParserNode> ntRanges;

  public CfgParserNode(int ntRuleIndex, int termRuleIndex, int start, int end,
      List<CfgParserNode> ntRanges) {
    super();
    this.ntRuleIndex = ntRuleIndex;
    this.termRuleIndex = termRuleIndex;
    this.start = start;
    this.end = end;
    this.ntRanges = ntRanges;
  }

  public int getEnd() {
    return end;
  }

  public List<CfgParserNode> getNtRanges() {
    return ntRanges;
  }

  public int getNtRuleIndex() {
    return ntRuleIndex;
  }

  public int getStart() {
    return start;
  }

  public int getTermRuleIndex() {
    return termRuleIndex;
  }

  public void setEnd(int end) {
    this.end = end;
  }

  public void setNtRanges(List<CfgParserNode> ntRanges) {
    this.ntRanges = ntRanges;
  }

  public void setNtRuleIndex(int ntRuleIndex) {
    this.ntRuleIndex = ntRuleIndex;
  }

  public void setOffset(int offset) {
    start += offset;
    end += offset;
    if (ntRanges != null) {
      for (CfgParserNode ntRange : ntRanges) {
        ntRange.setOffset(offset);
      }
    }
  }

  public void setStart(int start) {
    this.start = start;
  }

  public void setTermRuleIndex(int termRuleIndex) {
    this.termRuleIndex = termRuleIndex;
  }

  @Override
  public String toString() {
    return "CfgParserNode [ntRuleIndex=" + ntRuleIndex + ", termRuleIndex=" + termRuleIndex
        + ", start=" + start + ", end=" + end + ", ntRanges=" + ntRanges + "]";
  }
}
