package edu.isi.madcat.tmem.lextrans;

public class LexTranslatorConfig {
  private String modelFile;

  private int maxSegmentLength;

  private int maxRules;

  public LexTranslatorConfig() {
    super();
    this.modelFile = null;
    this.maxSegmentLength = 5;
    this.maxRules = 1;
  }

  public int getMaxRules() {
    return maxRules;
  }

  public int getMaxSegmentLength() {
    return maxSegmentLength;
  }

  public String getModelFile() {
    return modelFile;
  }

  public void setMaxRules(int maxRules) {
    this.maxRules = maxRules;
  }

  public void setMaxSegmentLength(int maxSegmentLength) {
    this.maxSegmentLength = maxSegmentLength;
  }

  public void setModelFile(String modelFile) {
    this.modelFile = modelFile;
  }

  @Override
  public String toString() {
    return "LexTranslatorConfig [modelFile=" + modelFile + ", maxSegmentLength=" + maxSegmentLength
        + ", maxRules=" + maxRules + "]";
  }

}
