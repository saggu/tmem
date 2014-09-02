package edu.isi.madcat.tmem.training.hmmaligner;

import edu.isi.madcat.tmem.utils.TextSegment;

public class SentPair {
  public TextSegment getSegment() {
    return segment;
  }

  public void setSegment(TextSegment segment) {
    this.segment = segment;
  }

  private TextSegment segment;
  private int[] source;
  private int[] target;

  public SentPair(TextSegment segment, int[] source, int[] target) {
    super();
    this.segment = segment;
    this.source = source;
    this.target = target;
  }

  public int[] getSource() {
    return source;
  }

  public int[] getTarget() {
    return target;
  }

  public void setSource(int[] source) {
    this.source = source;
  }

  public void setTarget(int[] target) {
    this.target = target;
  }

}
