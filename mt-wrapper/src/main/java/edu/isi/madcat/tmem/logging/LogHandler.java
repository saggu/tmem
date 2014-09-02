package edu.isi.madcat.tmem.logging;

public class LogHandler {
  public static void write(String str) {
    System.out.println(str);
  }

  public static void writeError(String str) {
    System.err.println(str);
  }
}
