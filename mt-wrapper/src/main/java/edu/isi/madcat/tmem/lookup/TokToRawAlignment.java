package edu.isi.madcat.tmem.lookup;

import java.util.Map;

import edu.isi.madcat.tmem.alignment.Range;
import edu.isi.madcat.tmem.alignment.TokenAlignment;

public class TokToRawAlignment {
  private Map<Range, Range> alignMap;

  public TokToRawAlignment(TokenAlignment alignment) {
    alignMap = alignment.createRangeMap();
  }

  public Map<Range, Range> getAlignMap() {
    return alignMap;
  }

  public Range projectRange(int inputStart, int inputEnd) {
    int start = -1;
    int end = -1;
    for (int i = inputStart; i <= inputEnd; i++) {
      Range rawRange = alignMap.get(new Range(i, i));
      if (rawRange != null) {
        if (start == -1 || rawRange.getStart() < start) {
          start = rawRange.getStart();
        }
        if (end == -1 || rawRange.getEnd() > end) {
          end = rawRange.getEnd();
        }
      }
    }
    if (start == -1 || end == -1) {
      return null;
    }
    return new Range(start, end);
  }

  public void setAlignMap(Map<Range, Range> alignMap) {
    this.alignMap = alignMap;
  }
}
