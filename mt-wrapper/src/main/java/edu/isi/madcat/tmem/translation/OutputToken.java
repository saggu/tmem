package edu.isi.madcat.tmem.translation;

public class OutputToken {
  private String word;

  private TokenJoiner joiner;

  public OutputToken(String word) {
    super();
    this.word = word;
    this.joiner = TokenJoiner.DEFAULT;
  }

  public OutputToken(String word, TokenJoiner joiner) {
    super();
    this.word = word;
    this.joiner = joiner;
  }

  public OutputToken(String word, String joinType) {
    super();
    this.word = word;
    this.joiner = TokenJoiner.fromString(joinType);
  }

  public TokenJoiner getJoiner() {
    return joiner;
  }

  public String getWord() {
    return word;
  }

  public void setJoiner(TokenJoiner joiner) {
    this.joiner = joiner;
  }

  public void setWord(String word) {
    this.word = word;
  }
}
