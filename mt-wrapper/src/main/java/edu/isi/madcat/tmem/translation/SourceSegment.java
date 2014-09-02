package edu.isi.madcat.tmem.translation;

public class SourceSegment {
  protected int id;
  protected String text;

  public SourceSegment(int id, String text) {
    super();
    this.id = id;
    this.text = text;
  }

  public int getId() {
    return id;
  }

  public String getText() {
    return text;
  }

}
