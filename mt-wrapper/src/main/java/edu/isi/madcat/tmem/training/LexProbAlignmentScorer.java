package edu.isi.madcat.tmem.training;

import java.util.List;

import edu.isi.madcat.tmem.alignment.AlignmentScorer;

public class LexProbAlignmentScorer extends AlignmentScorer {
  protected LexProbTable bwLexProbTable;
  protected LexProbTable fwLexProbTable;
  protected List<List<String>> source;
  protected List<List<String>> target;

  public LexProbAlignmentScorer(LexProbTable fwLexProbTable, LexProbTable bwLexProbTable) {
    super();
    this.fwLexProbTable = fwLexProbTable;
    this.bwLexProbTable = bwLexProbTable;
  }

  @Override
  public int lengthA() {
    return source.size();
  }

  @Override
  public int lengthB() {
    return target.size();
  }

  @Override
  public double score(int as, int ae, int bs, int be, double bestScore) {
    double fwLogProb = computeTranslationProb(source, target, as, ae, bs, be, fwLexProbTable);
    double bwLogProb = computeTranslationProb(target, source, bs, be, as, ae, bwLexProbTable);
    return fwLogProb + bwLogProb;
  }

  public void setInput(List<List<String>> source, List<List<String>> target) {
    this.source = source;
    this.target = target;
  }

  protected double computeTranslationProb(List<List<String>> source, List<List<String>> target,
      int as, int ae, int bs, int be, LexProbTable lexProbTable) {
    double totalProb = 0.0;
    for (int s1 = as; s1 <= ae; s1++) {
      List<String> sourceLine = source.get(s1);
      for (int s2 = 0; s2 < sourceLine.size(); s2++) {
        String sourceWord = sourceLine.get(s2);
        // double maxProb = lexProbTable.getProb(sourceWord, "NULL")*2.0;
        double maxProb = -10.0;
        // System.out.println(sourceWord +" "+"NULL"+" "+maxProb);
        for (int t1 = bs; t1 <= be; t1++) {
          List<String> targetLine = target.get(t1);
          for (int t2 = 0; t2 < targetLine.size(); t2++) {
            String targetWord = targetLine.get(t2);
            double prob = lexProbTable.getProb(sourceWord, targetWord);
            // System.out.println(sourceWord +" "+targetWord+" "+prob);
            if (prob > maxProb) {
              maxProb = prob;
            }
          }
        }
        totalProb += maxProb;
      }
    }
    return totalProb;
  }

}
