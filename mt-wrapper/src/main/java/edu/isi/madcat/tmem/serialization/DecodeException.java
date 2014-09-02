package edu.isi.madcat.tmem.serialization;

public class DecodeException extends Exception {
  static final long serialVersionUID = 1L;

  protected String message;

  public DecodeException() {
    super();
    this.message = "";
  }

  public DecodeException(String message) {
    super();
    this.message = message;
  }

  @Override
  public String getMessage() {
    return message;
  }

}
