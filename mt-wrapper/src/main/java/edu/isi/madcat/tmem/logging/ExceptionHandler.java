package edu.isi.madcat.tmem.logging;

public abstract class ExceptionHandler {
  private static ExceptionHandler instance = null;

  static {
    instance = new DebugExceptionHandler();
  }

  public static void handle(Throwable t) {
    instance.handleImpl(t);
  }

  public abstract void handleImpl(Throwable t);
}
