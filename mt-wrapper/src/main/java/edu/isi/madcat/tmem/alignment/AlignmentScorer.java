package edu.isi.madcat.tmem.alignment;

public abstract class AlignmentScorer {
  public boolean oneToManyOnly() {
    return oneToManyOnly;
  }

  public void setOneToManyOnly(boolean oneToManyOnly) {
    this.oneToManyOnly = oneToManyOnly;
  }

  protected int maxTokensPerChunk;

  protected boolean oneToManyOnly;
  
  public AlignmentScorer() {
    maxTokensPerChunk = 5;
    oneToManyOnly = false;
  }

  public boolean allowManyToMany() {
    return false;
  }

  public int getMaxTokensPerChunk() {
    return maxTokensPerChunk;
  }

  public abstract int lengthA();

  public abstract int lengthB();

  public double score(int as, int ae, int bs, int be) {
    return score(as, ae, bs, be, 1e30);
  }

  public abstract double score(int as, int ae, int bs, int be, double bestScore);

  public void setMaxTokensPerChunk(int maxTokensPerChunk) {
    this.maxTokensPerChunk = maxTokensPerChunk;
  }

  public double totalScore() {
    return score(0, lengthA() - 1, 0, lengthB() - 1, 1e30);
  }
}
