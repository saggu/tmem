package edu.isi.madcat.tmem.processors.cfg;

public class CfgInputToken {
  public enum TokenType {
    SOS, EOS, NORMAL
  }

  public static CfgInputToken createEndToken() {
    return new CfgInputToken(TokenType.EOS, "");
  }

  public static CfgInputToken createStartToken() {
    return new CfgInputToken(TokenType.SOS, "");
  }

  private TokenType type;

  private String text;

  public CfgInputToken(TokenType type, String text) {
    super();
    this.type = type;
    this.text = text;
  }

  public String getText() {
    return text;
  }

  public TokenType getType() {
    return type;
  }

  public void setText(String text) {
    this.text = text;
  }

  public void setType(TokenType type) {
    this.type = type;
  }

  @Override
  public String toString() {
    return "CfgInputToken [type=" + type + ", text=" + text + "]";
  }
}
