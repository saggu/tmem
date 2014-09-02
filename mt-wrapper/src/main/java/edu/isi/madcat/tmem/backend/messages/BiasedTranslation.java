package edu.isi.madcat.tmem.backend.messages;

import java.util.List;

import edu.isi.madcat.tmem.serialization.SimpleSerializable;

public class BiasedTranslation extends SimpleSerializable {
  protected Long sourceIndex;

  protected List<String> targetWords;

  public BiasedTranslation() {
    super();
  }

  public BiasedTranslation(Long sourceIndex, List<String> targetWords) {
    super();
    this.sourceIndex = sourceIndex;
    this.targetWords = targetWords;
  }

  public Long getSourceIndex() {
    return sourceIndex;
  }

  public List<String> getTargetWords() {
    return targetWords;
  }

}
