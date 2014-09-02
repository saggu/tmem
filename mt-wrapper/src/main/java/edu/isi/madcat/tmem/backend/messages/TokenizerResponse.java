package edu.isi.madcat.tmem.backend.messages;

import java.util.List;

import edu.isi.madcat.tmem.serialization.SimpleSerializable;

public class TokenizerResponse extends SimpleSerializable {
  protected List<String> tokens;
  protected List<WordAlignment> alignment;
  
  public TokenizerResponse() {

  }

  public TokenizerResponse(List<String> tokens) {
    super();
    this.tokens = tokens;
  }
  
  public TokenizerResponse(List<String> tokens, List<WordAlignment> alignment) {
    super();
    this.tokens = tokens;
    this.alignment = alignment;
  }

  public List<String> getTokens() {
    return tokens;
  }

  public List<WordAlignment> getAlignment() {
    return alignment;
  }
}
