package edu.isi.madcat.tmem.backend.messages;

import java.util.List;

import edu.isi.madcat.tmem.serialization.SimpleSerializable;

public class SegmentorRequest extends SimpleSerializable {
  protected List<String> sourceWords;

  public SegmentorRequest() {

  }

  public SegmentorRequest(List<String> sourceWords) {
    super();
    this.sourceWords = sourceWords;
  }

  public List<String> getSourceWords() {
    return sourceWords;
  }

}
