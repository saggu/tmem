package edu.isi.madcat.tmem.ter;

import java.util.List;

import edu.isi.madcat.tmem.alignment.AlignmentScorer;

public class TerAlignmentScorer extends AlignmentScorer {
  protected List<List<String>> a;
  protected List<List<String>> b;

  public TerAlignmentScorer(List<List<String>> a, List<List<String>> b) {
    super();
    this.a = a;
    this.b = b;
  }

  @Override
  public boolean allowManyToMany() {
    return false;
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
    String[] subA = getSubArray(a, as, ae);
    String[] subB = getSubArray(b, bs, be);

    double numEdits = TerCalc.terLowerBoundEdits(subA, subB);
    // TerAlignment alignment = TerCalc.TER(subA, subB);
    // System.out.println(Arrays.toString(subA));
    // System.out.println(Arrays.toString(subB));
    // System.out.println(numEdits);
    return numEdits;
  }

  protected String[] getSubArray(List<List<String>> v, int s, int e) {
    int length = 0;
    for (int i = s; i <= e; i++) {
      length += v.get(i).size();
    }

    String[] output = new String[length];
    int index = 0;
    for (int i = s; i <= e; i++) {
      for (int j = 0; j < v.get(i).size(); j++) {
        output[index] = v.get(i).get(j);
        index++;
      }
    }
    return output;
  }
}
