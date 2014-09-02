package edu.isi.madcat.tmem.alignment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NgramOverlapScorer extends AlignmentScorer {
  public final static int DEFAULT_NGRAM_ORDER = 3;

  public static double computeAverageOverlap(NgramCounts countsA, NgramCounts countsB) {
    double totalOverlap = 0.0;
    double totalCount = 0.0;
    int length = Math.max(countsA.getLength(), countsB.getLength());
    for (int i = 1; i <= countsA.getNgramOrder(); i++) {
      int numNgrams = length - (i - 1);
      if (numNgrams < 1) {
        continue;
      }
      Map<String, Double> ngramsA = countsA.getNgrams(i);
      Map<String, Double> ngramsB = countsB.getNgrams(i);
      double overlap = computeNgramOverlap(ngramsA, ngramsB);
      double overlapPercent = (overlap / numNgrams);
      totalOverlap += overlapPercent;
      totalCount += 1.0;
    }
    if (totalCount == 0.0) {
      return 1e30;
    }
    return totalOverlap / length;
  }

  public static double computeNgramOverlap(Map<String, Double> ngramsA, Map<String, Double> ngramsB) {
    double totalCount = 0.0;
    for (Map.Entry<String, Double> entry : ngramsA.entrySet()) {
      Double countA = entry.getValue();
      Double countB = ngramsB.get(entry.getKey());
      if (countB != null) {
        double count = Math.min(countA.doubleValue(), countB.doubleValue());
        totalCount += count;
      }
    }
    return totalCount;
  }

  protected List<List<String>> a;

  protected List<List<String>> b;

  protected int ngramOrder;

  public NgramOverlapScorer(List<List<String>> a, List<List<String>> b) {
    super();
    this.a = a;
    this.b = b;
    this.ngramOrder = NgramOverlapScorer.DEFAULT_NGRAM_ORDER;
  }

  public NgramOverlapScorer(List<List<String>> a, List<List<String>> b, int ngramOrder) {
    super();
    this.a = a;
    this.b = b;
    this.ngramOrder = ngramOrder;
  }

  protected NgramOverlapScorer() {

  }

  @Override
  public int lengthA() {
    return a.size();
  }

  @Override
  public int lengthB() {
    return b.size();
  }

  @Override
  public double score(int as, int ae, int bs, int be, double bestScore) {
    List<String> subA = getSubList(a, as, ae);
    List<String> subB = getSubList(b, bs, be);

    NgramCounts countsA = new NgramCounts(subA, ngramOrder);
    NgramCounts countsB = new NgramCounts(subB, ngramOrder);
    return -computeAverageOverlap(countsA, countsB) * subA.size();
  }

  protected List<String> getSubList(List<List<String>> v, int s, int e) {
    List<String> output = new ArrayList<String>();
    for (int i = s; i <= e; i++) {
      for (int j = 0; j < v.get(i).size(); j++) {
        output.add(v.get(i).get(j));
      }
    }
    return output;
  }

}
