package edu.isi.madcat.tmem.backend.messages;

import edu.isi.madcat.tmem.serialization.SimpleSerializable;

public class TokenizerRequest extends SimpleSerializable {
  protected String language;

  protected String text;

  public TokenizerRequest() {

  }

  public TokenizerRequest(String language, String text) {
    super();
    this.language = language;
    this.text = text;
  }

  public String getLanguage() {
    return language;
  }

  public String getText() {
    return text;
  }
}
