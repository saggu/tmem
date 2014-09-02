package edu.isi.madcat.tmem.training.hmmaligner;

public class HmmParams {
  HmmConfig config;
  int maxLength;
  TransTable transTable;

  double[] initCount;
  double[] termCount;
  double[] distortionCount;

  public HmmParams(HmmConfig config) {
    this.config = config;
    initialize();
  }

  public HmmParams(HmmParams prevHmmParams) {
    this.config = prevHmmParams.config;
    initialize();
  }

  public void countEmission(SentPairProbs sp, int sourceIndex, int targetIndex, double count) {
    int sourceId = sp.getSent().getSource()[sourceIndex];
    int targetId = sp.getSent().getTarget()[targetIndex];

    if (count > 0.0) {
      transTable.increment(sourceId, targetId, count);
    }
  }

  public void countTransition(SentPairProbs sp, int start, int end, int target, double count) {
    if (start == 0) {
      initCount[end] += count;
    } else if (end == sp.getSent().getSource().length) {
      termCount[end - start] += count;
    } else {
      distortionCount[end - start + maxLength] += count;
    }
  }

  public void createInitialProbs(TransTable transTable) {
    this.transTable = transTable;
    for (int i = 0; i < initCount.length; i++) {
      initCount[i] = 1.0;
    }
    for (int i = 0; i < termCount.length; i++) {
      termCount[i] = 1.0;
    }
    for (int i = 0; i < distortionCount.length; i++) {
      distortionCount[i] = 1.0;
    }
  }

  public void setSentPairProbs(SentPairProbs sp) {
    int[] source = sp.getSent().getSource();
    int[] target = sp.getSent().getTarget();
    double[][] emission = sp.getEmission();
    double[][] transition = sp.getTransition();

    int sourceLength = source.length - 1;
    int targetLength = target.length - 1;

    for (int i = 0; i <= sourceLength; i++) {
      double normalization = 0.0;
      for (int j = 1; j <= targetLength; j++) {
        double emissionProb = 0.0;
        if (transTable == null) {
          emissionProb = 1.0;
        } else {
          emissionProb = transTable.getEmission(source[i], target[j]);
        }
        emission[i][j] = emissionProb;
        normalization += emission[i][j];
      }

      if (normalization > 0.0) {
        for (int j = 1; j <= targetLength; j++) {
          emission[i][j] /= normalization;
        }
      }
    }

    transition[0][sourceLength + 1] = 0.0; // should never happen

    double normalization = 0.0;
    for (int ii = 1; ii <= sourceLength; ii++) {
      transition[0][ii] = initCount[ii];
      normalization += transition[0][ii];
    }
    if (normalization > 0.0) {
      for (int ii = 1; ii <= sourceLength; ii++) {
        transition[0][ii] /= normalization;
      }
    }

    for (int i = 1; i <= sourceLength; i++) {
      int c = 0;
      normalization = 0.0;
      for (int ii = 1; ii <= sourceLength; ii++) {
        transition[i][ii] = distortionCount[ii - i + maxLength];
        normalization += transition[i][ii];
      }

      transition[i][sourceLength + 1] = termCount[(sourceLength + 1) - i];
      normalization += transition[i][sourceLength + 1];

      if (normalization > 0.0) {
        for (int ii = 1; ii <= sourceLength + 1; ii++) {
          transition[i][ii] /= normalization;
        }
      }
    }
  }

  void initialize() {
    transTable = new TransTable();
    maxLength = config.getMaxSentLength();

    distortionCount = new double[2 * (config.getMaxSentLength() + 2)];
    initCount = new double[config.getMaxSentLength() + 2];
    termCount = new double[config.getMaxSentLength() + 2];
  }

  void normalize() {
    transTable.normalize();
  }

  void reset() {
    for (int i = 0; i < initCount.length; i++) {
      initCount[i] = 0.0;
    }
    for (int i = 0; i < termCount.length; i++) {
      termCount[i] = 0.0;
    }
    for (int i = 0; i < distortionCount.length; i++) {
      distortionCount[i] = 0.0;
    }
  }
}
