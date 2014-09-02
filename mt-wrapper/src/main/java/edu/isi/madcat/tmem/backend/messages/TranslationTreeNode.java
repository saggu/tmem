package edu.isi.madcat.tmem.backend.messages;

import java.util.List;

import edu.isi.madcat.tmem.serialization.SimpleSerializable;

public class TranslationTreeNode extends SimpleSerializable {
  protected List<WordAlignment> alignment;

  protected List<Long> ntNodeIndexes;

  protected Double score;

  protected List<Long> sourceIndexes;

  protected List<String> targetWords;

  public TranslationTreeNode() {

  }

  public TranslationTreeNode(List<Long> sourceIndexes, List<String> targetWords,
      List<Long> ntNodeIndexes, List<WordAlignment> alignment, Double score) {
    super();
    this.sourceIndexes = sourceIndexes;
    this.targetWords = targetWords;
    this.ntNodeIndexes = ntNodeIndexes;
    this.alignment = alignment;
    this.score = score;
  }

  public List<WordAlignment> getAlignment() {
    return alignment;
  }

  public List<Long> getNtNodeIndexes() {
    return ntNodeIndexes;
  }

  public Double getScore() {
    return score;
  }

  public List<Long> getSourceIndexes() {
    return sourceIndexes;
  }

  public List<String> getTargetWords() {
    return targetWords;
  }

  public void setAlignment(List<WordAlignment> alignment) {
    this.alignment = alignment;
  }

  public void setNtNodeIndexes(List<Long> ntNodeIndexes) {
    this.ntNodeIndexes = ntNodeIndexes;
  }

  public void setScore(Double score) {
    this.score = score;
  }

  public void setSourceIndexes(List<Long> sourceIndexes) {
    this.sourceIndexes = sourceIndexes;
  }

  public void setTargetWords(List<String> targetWords) {
    this.targetWords = targetWords;
  }
}
