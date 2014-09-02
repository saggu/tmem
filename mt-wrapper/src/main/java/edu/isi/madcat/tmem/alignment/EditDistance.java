package edu.isi.madcat.tmem.alignment;

public class EditDistance {

  protected double deleteCost;
  protected double insertCost;
  protected double subCost;

  public EditDistance() {
    insertCost = 1.0;
    deleteCost = 1.0;
    subCost = 1.0;
  }

  public EditDistance(double insertCost, double deleteCost, double subCost) {
    super();
    this.insertCost = insertCost;
    this.deleteCost = deleteCost;
    this.subCost = subCost;
  }

  public double compute(String s, String t) {
    return compute(s, t, 1e30);
  }

  public double compute(String s, String t, double bestScore) {
    int m = s.length();
    int n = t.length();
    double[] prevRow = new double[m + 1];
    double[] curRow = new double[m + 1];
    for (int i = 0; i < prevRow.length; i++) {
      prevRow[i] = i;
    }
    for (int i = 0; i < curRow.length; i++) {
      curRow[i] = 0.0;
    }
    for (int j = 0; j < n; j++) {
      curRow[0] = j + 1;
      for (int i = 0; i < m; i++) {
        if (s.charAt(i) == t.charAt(j)) {
          curRow[i + 1] = prevRow[i];
        } else {
          curRow[i + 1] =
              Math.min(curRow[i] + deleteCost, Math.min(prevRow[i + 1] + insertCost, prevRow[i]
                  + subCost));
        }
      }
      double minScore = 1e30;
      for (int i = 0; i < curRow.length; i++) {
        prevRow[i] = curRow[i];
        if (curRow[i] < minScore) {
          minScore = curRow[i];
        }
      }
      // We can't do any better than minScore, so break out early if it's worse than our current
      // best score bestScore
      if (minScore > bestScore) {
        return 1e30;
      }
    }
    return curRow[m];
  }

  public double computeNormalized(String s, String t) {
    double length = s.length();
    double distance = compute(s, t);
    if (length > 0) {
      distance = distance / length;
    }
    if (distance > 1.0) {
      distance = 1.0;
    }
    return distance;
  }
}
