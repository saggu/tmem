package edu.isi.madcat.tmem.translation;

public class AltPhrase {
  private String targetString;

  public AltPhrase(String targetString) {
    super();
    this.targetString = targetString;
  }

  public String getTargetString() {
    return targetString;
  }

  public void setTargetString(String targetString) {
    this.targetString = targetString;
  }

  @Override
  public String toString() {
    return "AltPhrase [targetString=" + targetString + "]";
  }
}
