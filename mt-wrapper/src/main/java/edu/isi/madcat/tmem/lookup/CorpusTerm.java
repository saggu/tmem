package edu.isi.madcat.tmem.lookup;

public class CorpusTerm {
  private String rawSource;

  private String tokSource;

  private String rawTarget;

  private String tokTarget;

  private int sourceStart;
  
  private int sourceEnd;

  private int targetStart;

  private int targetEnd;

  public CorpusTerm(String raw, String tok, boolean isBackwards) {
    if (isBackwards) {
      this.rawSource = null;
      this.tokSource = null;
      this.rawTarget = raw;
      this.tokTarget = tok;
    } else {
      this.rawSource = raw;
      this.tokSource = tok;
      this.rawTarget = null;
      this.tokTarget = null;
    }
  }

  public CorpusTerm(String rawSource, String tokSource, String rawTarget, String tokTarget) {
    super();
    this.rawSource = rawSource;
    this.tokSource = tokSource;
    this.rawTarget = rawTarget;
    this.tokTarget = tokTarget;
  }

  public String getRawSource() {
    return rawSource;
  }

  public String getRawTarget() {
    return rawTarget;
  }

  public int getSourceEnd() {
    return sourceEnd;
  }

  public int getSourceStart() {
    return sourceStart;
  }

  public int getTargetEnd() {
    return targetEnd;
  }

  public int getTargetStart() {
    return targetStart;
  }

  public String getTokJoint() {
    return getJointString(getTokSource(), getTokTarget());
  }
  
  public static String getJointString(String source, String target) {
    return "SOURCE=" + source + "; TARGET=" + target;
  }

  public String getTokSource() {
    return tokSource;
  }

  public String getTokTarget() {
    return tokTarget;
  }

  public void setRawSource(String rawSource) {
    this.rawSource = rawSource;
  }

  public void setRawTarget(String rawTarget) {
    this.rawTarget = rawTarget;
  }

  public void setSourceEnd(int sourceEnd) {
    this.sourceEnd = sourceEnd;
  }

  public void setSourceStart(int sourceStart) {
    this.sourceStart = sourceStart;
  }

  public void setTargetEnd(int targetEnd) {
    this.targetEnd = targetEnd;
  }

  public void setTargetStart(int targetStart) {
    this.targetStart = targetStart;
  }

  public void setTokSource(String tokSource) {
    this.tokSource = tokSource;
  }

  public void setTokTarget(String tokTarget) {
    this.tokTarget = tokTarget;
  }
}
