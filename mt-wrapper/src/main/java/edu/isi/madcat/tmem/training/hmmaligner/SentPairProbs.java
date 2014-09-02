package edu.isi.madcat.tmem.training.hmmaligner;

public class SentPairProbs {
  private SentPair sent;

  private double[][] emission;

  private double[][] transition;

  public SentPairProbs(HmmConfig config) {
    super();
    this.sent = null;
    this.emission = new double[config.getMaxSentLength() + 1][config.getMaxSentLength() + 1];
    this.transition = new double[config.getMaxSentLength() + 1][config.getMaxSentLength() + 1];
  }

  public SentPairProbs(SentPair sent, double[][] emission, double[][] transition) {
    super();
    this.sent = sent;
    this.emission = emission;
    this.transition = transition;
  }

  public double[][] getEmission() {
    return emission;
  }

  public SentPair getSent() {
    return sent;
  }

  public double[][] getTransition() {
    return transition;
  }

  public void setEmission(double[][] emission) {
    this.emission = emission;
  }

  public void setSent(SentPair sent) {
    this.sent = sent;
  }

  public void setTransition(double[][] transition) {
    this.transition = transition;
  }

  public String toString(CorpusLexicon sourceLexicon, CorpusLexicon targetLexicon) {
    StringBuilder sb = new StringBuilder();
    sb.append("source: ");
    for (int i = 0; i < sent.getSource().length; i++) {
      if (i > 0) {
        sb.append(" ");
      }
      int id = sent.getSource()[i];
      sb.append(sourceLexicon.getWord(id) + "(" + id + ")");
    }
    sb.append("\n");
    sb.append("target: ");
    for (int i = 0; i < sent.getTarget().length; i++) {
      if (i > 0) {
        sb.append(" ");
      }
      int id = sent.getTarget()[i];
      sb.append(targetLexicon.getWord(id) + "(" + id + ")");
    }
    sb.append("\n");
    for (int i = 0; i < sent.getSource().length; i++) {
      int sourceId = sent.getSource()[i];
      for (int j = 0; j < sent.getTarget().length; j++) {
        int targetId = sent.getTarget()[j];
        sb.append("E: " + i + " " + sourceLexicon.getWord(sourceId) + "(" + sourceId + ")" + " "
            + j + " " + targetLexicon.getWord(targetId) + "(" + targetId + ") " + emission[i][j]
            + "\n");
      }
    }
    for (int i = 0; i < sent.getSource().length + 1; i++) {
      for (int j = 0; j < sent.getSource().length + 1; j++) {
        sb.append("T: " + i + " " + j + " " + transition[i][j] + "\n");
      }
    }
    return sb.toString();
  }
}
