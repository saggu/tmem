package edu.isi.madcat.tmem.exceptions;

import edu.isi.madcat.tmem.utils.Utils;

public class CateProcessException extends Exception {
  static final long serialVersionUID = 1L;

  protected String message;

  public CateProcessException() {
    super();
    this.message = "";
  }

  public CateProcessException(String message) {
    super();
    this.message = message;
  }

  public CateProcessException(String message, Throwable e) {
    super();
    this.message = message;
    this.message += e.getMessage() + Utils.getStackTrace(e);
  }

  @Override
  public String getMessage() {
    return message;
  }
}
