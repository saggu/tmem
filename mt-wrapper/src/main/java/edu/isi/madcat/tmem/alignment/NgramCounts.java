package edu.isi.madcat.tmem.alignment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NgramCounts {
  protected int length;
  protected int ngramOrder;
  protected List<Map<String, Double>> ngrams;

  public NgramCounts(List<String> words) {
    initialize(words, NgramOverlapScorer.DEFAULT_NGRAM_ORDER);
  }

  public NgramCounts(List<String> words, int ngramOrder) {
    initialize(words, ngramOrder);
  }

  public int getLength() {
    return length;
  }

  public int getNgramOrder() {
    return ngramOrder;
  }

  public Map<String, Double> getNgrams(int i) {
    return ngrams.get(i - 1);
  }

  private void initialize(List<String> words, int ngramOrder) {
    ngrams = new ArrayList<Map<String, Double>>();
    for (int i = 0; i < ngramOrder; i++) {
      ngrams.add(getNgrams(words, i + 1));
    }
    this.ngramOrder = ngramOrder;
    this.length = words.size();
  }

  protected Map<String, Double> getNgrams(List<String> x, int n) {
    Map<String, Double> counts = new HashMap<String, Double>();
    for (int i = 0; i < x.size() - n + 1; i++) {
      StringBuilder sb = new StringBuilder();
      for (int j = 0; j < n; j++) {
        if (j > 0) {
          sb.append(" ");
        }
        sb.append(x.get(i + j));
      }
      String ngram = sb.toString();
      double count = 0.0;
      Double currentCount = counts.get(ngram);
      if (currentCount != null) {
        count += currentCount.doubleValue();
      }
      count += 1.0;
      counts.put(ngram, new Double(count));
    }
    return counts;
  }
}
