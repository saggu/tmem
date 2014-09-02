package edu.isi.madcat.tmem.backend.messages;

import java.util.List;

import edu.isi.madcat.tmem.serialization.SimpleSerializable;

public class AuxiliaryRule extends SimpleSerializable {
  @Override
  public String toString() {
    return "AuxiliaryRule [alignment=" + alignment + ", score=" + score + ", sourceEnd="
        + sourceEnd + ", sourceStart=" + sourceStart + ", targetWords=" + targetWords + "]";
  }

  protected List<WordAlignment> alignment;
  protected Double score;
  protected Long sourceEnd;
  protected Long sourceStart;
  protected List<String> targetWords;

  public AuxiliaryRule() {

  }

  public AuxiliaryRule(Long sourceStart, Long sourceEnd, List<String> targetWords,
      List<WordAlignment> alignment, Double score) {
    super();
    this.sourceStart = sourceStart;
    this.sourceEnd = sourceEnd;
    this.targetWords = targetWords;
    this.alignment = alignment;
    this.score = score;
  }

}
