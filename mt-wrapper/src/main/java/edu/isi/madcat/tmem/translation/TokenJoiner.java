package edu.isi.madcat.tmem.translation;

public enum TokenJoiner {
  DEFAULT, LEFT, RIGHT;
  
  public static TokenJoiner fromString(String joinType) {
    if (joinType == null || joinType.equals("") || joinType.equals("D")) {
      return TokenJoiner.DEFAULT;
    }
    if (joinType.equals("L")) {
      return TokenJoiner.LEFT;
    }
    if (joinType.equals("R")) {
      return TokenJoiner.RIGHT;
    }
    throw new RuntimeException("Unknown join type: " + joinType);
  }
}
