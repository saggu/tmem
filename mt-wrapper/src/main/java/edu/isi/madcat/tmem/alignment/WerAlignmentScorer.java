package edu.isi.madcat.tmem.alignment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WerAlignmentScorer extends AlignmentScorer {
  protected static EditDistance editDistance;
  static {
    editDistance = new EditDistance();
  }

  protected List<String> a;

  protected List<String> b;
  protected Map<Character, Character> charMap = null;

  protected Map<String, String> tokenMap = null;

  public WerAlignmentScorer() {
    super();
    initializeMapping();
  }

  public WerAlignmentScorer(List<String> a, List<String> b) {
    super();
    initializeMapping();
    this.a = convertForHeuristic(a);
    this.b = convertForHeuristic(b);
  }

  public WerAlignmentScorer(String wordA, String wordB) {
    super();
    initializeMapping();
    this.a = new ArrayList<String>();
    this.a.add(wordA);
    this.b = new ArrayList<String>();
    this.b.add(wordB);
    this.a = convertForHeuristic(this.a);
    this.b = convertForHeuristic(this.b);
  }

  @Override
  public int lengthA() {
    return a.size();
  }

  @Override
  public int lengthB() {
    return b.size();
  }

  public double score() {
    String s = makeFlatString(a, 0, a.size() - 1);
    String t = makeFlatString(b, 0, b.size() - 1);
    return editDistance.compute(s, t, 1e30);
  }

  @Override
  public double score(int as, int ae, int bs, int be, double bestScore) {
    String s = makeFlatString(a, as, ae);
    String t = makeFlatString(b, bs, be);
    return editDistance.compute(s, t, bestScore);
  }

  protected List<String> convertForHeuristic(List<String> input) {
    List<String> output = new ArrayList<String>();
    for (int i = 0; i < input.size(); i++) {
      String inputString = input.get(i);
      String mappedString = tokenMap.get(inputString);
      if (mappedString != null) {
        output.add(mappedString);
      } else {
        StringBuilder b = new StringBuilder();
        for (int j = 0; j < inputString.length(); j++) {
          char inputChar = inputString.charAt(j);
          Character mappedChar = charMap.get(inputChar);
          if (mappedChar != null) {
            b.append(mappedChar);
          } else {
            b.append(inputChar);
          }
        }
        output.add(b.toString());
      }
    }
    return output;
  }

  protected void initializeMapping() {
    tokenMap = new HashMap<String, String>();
    tokenMap.put("-", "@-@");
    tokenMap.put("@-", "-");
    tokenMap.put("-", "-@");
    tokenMap.put(":", "@:@");
    tokenMap.put(":", ":");
    tokenMap.put(":", ":@");

    charMap = new HashMap<Character, Character>();
    for (int i = 'a'; i < 'z'; i++) {
      charMap.put((char) i, Character.toUpperCase((char) i));
    }
  }

  protected String makeFlatString(List<String> tokens, int s, int e) {
    StringBuilder builder = new StringBuilder();
    for (int i = s; i <= e; i++) {
      builder.append(tokens.get(i));
    }
    return builder.toString();
  }

}
