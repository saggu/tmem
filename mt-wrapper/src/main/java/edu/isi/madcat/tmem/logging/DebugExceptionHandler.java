package edu.isi.madcat.tmem.logging;

import java.io.PrintWriter;
import java.io.StringWriter;

public class DebugExceptionHandler extends ExceptionHandler {
  @Override
  public void handleImpl(Throwable t) {
    StringWriter writer = new StringWriter();
    writer.write("Exception");
    PrintWriter pw = new PrintWriter(writer);
    t.printStackTrace(pw);
    LogHandler.writeError(t.getMessage() + "\n" + writer.toString());
    System.exit(1);
  }
}
