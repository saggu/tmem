package edu.isi.madcat.tmem.alignment;

class DistancePair {
  // Array "A" - End
  protected int ae;
  // Array "A" - Start
  protected int as;
  // Array "B" - End
  protected int be;
  // Array "B" - Start
  protected int bs;

  protected DistancePair prev;

  protected double score;

  public DistancePair(int bs, int be, int as, int ae, double score, DistancePair prev) {
    super();
    this.bs = bs;
    this.be = be;
    this.as = as;
    this.ae = ae;
    this.score = score;
    this.prev = prev;
  }

  public int getAe() {
    return ae;
  }

  public int getAs() {
    return as;
  }

  public int getBe() {
    return be;
  }

  public int getBs() {
    return bs;
  }

  public DistancePair getPrev() {
    return prev;
  }

  public double getScore() {
    return score;
  }

}
