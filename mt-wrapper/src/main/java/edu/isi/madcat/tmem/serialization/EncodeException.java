package edu.isi.madcat.tmem.serialization;

public class EncodeException extends Exception {
  static final long serialVersionUID = 1L;

  protected String message;

  public EncodeException() {
    super();
    this.message = "";
  }

  public EncodeException(String message) {
    super();
    this.message = message;
  }

  @Override
  public String getMessage() {
    return message;
  }

}
