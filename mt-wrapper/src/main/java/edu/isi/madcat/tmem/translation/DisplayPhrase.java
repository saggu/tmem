package edu.isi.madcat.tmem.translation;

import java.util.List;

import edu.isi.madcat.tmem.alignment.Range;

public class DisplayPhrase {
  /*
   *  Character range of the parent TranslatedSegment.inputString. Can be multiple ranges for discontinuous alignments.
   */
  private List<Range> spans;

  /*
   * The detokenized, translated phrase
   */
  private String targetString;

  /*
   * List of alternative phrases if they have been generated. null if they have not been.
   */
  private List<AltPhrase> altPhrases;

  private TokenJoiner tokenJoiner;

  public DisplayPhrase(List<Range> spans, String targetString) {
    super();
    this.spans = spans;
    this.targetString = targetString;
    this.altPhrases = null;
    this.tokenJoiner = TokenJoiner.DEFAULT;
  }

  public List<AltPhrase> getAltPhrases() {
    return altPhrases;
  }
  
  public List<Range> getSpans() {
    return spans;
  }

  public String getTargetString() {
    return targetString;
  }
  
  public TokenJoiner getTokenJoiner() {
    return tokenJoiner;
  }
  
  public void setAltPhrases(List<AltPhrase> altPhrases) {
    this.altPhrases = altPhrases;
  }

  public void setSpans(List<Range> spans) {
    this.spans = spans;
  }

  public void setTargetString(String targetString) {
    this.targetString = targetString;
  }

  public void setTokenJoiner(TokenJoiner tokenJoiner) {
    this.tokenJoiner = tokenJoiner;
  }

  @Override
  public String toString() {
    return "DisplayPhrase [spans=" + spans + ", targetString=" + targetString + ", altPhrases="
        + altPhrases + "]";
  }
}
