package edu.isi.madcat.tmem.alignment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.isi.madcat.tmem.tokenize.CharacterTokenizer;
import edu.isi.madcat.tmem.tokenize.Tokenizer;

public class AlignmentGenerator {

  public static int MAX_TOKENS_PER_CHUNK = 5;

  /**
   * Aligns a to b in a left-to-right in a proportional fashion based on the count ratio. Only use
   * this if {@link #heuristicAlignment(AlignmentScorer) heuristicAlignment} fails.
   * 
   * For example: <br/>
   * 
   * <pre>
   * a: a1 a2
   * b: b1 b2 b3 b4 b5
   * 
   * Alignment:
   * a1 => b1 b2 b3
   * a2 => b4 b5
   * </pre>
   * 
   * @param a First set of tokens to be aligned
   * @param b Second set of tokens to be aligned
   * @return TokenAlignment between a and b, guaranteed to be non-null.
   */
  public static TokenAlignment backoffAlignment(List<String> a, List<String> b) {
    TokenAlignment alignment = new TokenAlignment();
    if (a.size() == 0 || b.size() == 0) {
      return alignment;
    }
    boolean reverse = false;
    if (a.size() >= b.size()) {
      reverse = true;
      List<String> tmp = a;
      a = b;
      b = tmp;
    }

    int b_per_a = b.size() / a.size();
    int extra = b.size() % a.size();
    int k = 0;
    for (int i = 0; i < a.size(); i++) {
      int n = b_per_a;
      if (i < extra) {
        n++;
      }

      Range aRange = new Range(i, i);
      Range bRange = new Range(k, k + n - 1);
      if (!reverse) {
        alignment.add(new AlignmentPair(aRange, bRange));
      } else {
        alignment.add(new AlignmentPair(bRange, aRange));
      }
      k += n;
    }
    alignment.sort();
    return alignment;
  }

  /**
   * Aligns a to b to minimize the global character edit distance of aligned spans. All
   * AlignmentPairs will be one-to-many or many-to-one, not many-to-many or null alignments.
   * 
   */

  public static TokenAlignment heuristicAlignment(AlignmentScorer s) {
    // If either of the inputs are empty, we just return an empty structure
    if (s.lengthA() == 0 || s.lengthB() == 0) {
      return new TokenAlignment();
    }
    int maxTokensPerChunk = s.getMaxTokensPerChunk();
    ArrayList<ArrayList<DistancePair>> bestDistance = new ArrayList<ArrayList<DistancePair>>();
    for (int i = 0; i < s.lengthA() + 1; i++) {
      bestDistance.add(new ArrayList<DistancePair>());
    }
    ArrayList<DistancePair> start = bestDistance.get(0);
    start.add(new DistancePair(-1, -1, -1, -1, 0.0, null));

    for (int ae = 0; ae < s.lengthA(); ae++) {
      HashMap<Integer, DistancePair> distanceMap = new HashMap<Integer, DistancePair>();
      for (int as = ae; as >= 0 && as > ae - maxTokensPerChunk; as--) {
        if (as != ae && s.oneToManyOnly()) {
          continue;
        }
        for (DistancePair prevDist : bestDistance.get(as)) {
          int maxTokens = maxTokensPerChunk;
          if (ae != as && !s.allowManyToMany()) {
            maxTokens = 1;
          }
          int bs = prevDist.getBe() + 1;
          for (int be = bs; be < s.lengthB() && (be < (bs + maxTokens)); be++) {
            // If we've reached the last token in one of the strings, we must also have reached the
            // last token in the other string, or else the alignment is invalid
            if ((be == s.lengthB() - 1 && ae != s.lengthA() - 1)
                || (be != s.lengthB() - 1 && ae == s.lengthA() - 1)) {
              continue;
            }
            DistancePair bestDist = distanceMap.get(be);
            double bestScore = 1e30;
            if (bestDist != null) {
              bestScore = bestDist.getScore();
            }

            double localScore = s.score(as, ae, bs, be, bestScore);
            double newScore = prevDist.getScore() + localScore;
            if (bestDist == null || newScore < bestDist.getScore()) {
              DistancePair newDist = new DistancePair(bs, be, as, ae, newScore, prevDist);
              distanceMap.put(be, newDist);
            }
          }
        }
      }
      ArrayList<DistancePair> dps = bestDistance.get(ae + 1);
      for (Map.Entry<Integer, DistancePair> entry : distanceMap.entrySet()) {
        DistancePair dist = entry.getValue();
        dps.add(dist);
      }
    }
    TokenAlignment alignment = new TokenAlignment();
    ArrayList<DistancePair> finalDistances = bestDistance.get(s.lengthA());
    if (finalDistances.size() == 0) {
      return null;
    }
    if (finalDistances.size() != 1 || finalDistances.get(0).getBe() != s.lengthB() - 1) {
      throw new RuntimeException("Invalid alignment.");
    }
    DistancePair p = finalDistances.get(0);
    while (p != null) {
      if (p.getAe() >= 0) {
        alignment.add(new AlignmentPair(new Range(p.getAs(), p.getAe()), new Range(p.getBs(), p
            .getBe())));
      }
      p = p.getPrev();
    }
    alignment.sort();
    return alignment;
  }

  public static TokenAlignment oneToOneAlignment(List<String> input) {
    TokenAlignment alignment = new TokenAlignment();
    for (int i = 0; i < input.size(); i++) {
      alignment.add(i, i, i, i);
    }
    return alignment;
  }

  public static double scoreAlignment(TokenAlignment alignment, AlignmentScorer scorer) {
    double totalScore = 0.0;
    for (AlignmentPair p : alignment) {
      double localScore =
          scorer.score(p.getInput().getStart(), p.getInput().getEnd(), p.getOutput().getStart(), p
              .getOutput().getEnd());
      totalScore += localScore;
    }
    return totalScore;
  }

  public static TokenAlignment stringToWordAlignment(String input, List<String> words) {
    return stringToWordAlignment(input, words, CharacterTokenizer.WHITESPACE);
  }

  public static TokenAlignment stringToWordAlignment(String input, List<String> words,
      Tokenizer tokenizer) {
    TokenAlignment inputToRawAlign = new TokenAlignment();
    List<String> rawTokens = tokenizer.tokenize(input, inputToRawAlign);
    TokenAlignment rawToWordAlign = wordToWordAlignment(rawTokens, words);
    TokenAlignment inputToWordAlign =
        TokenAlignment.projectAlignment(inputToRawAlign, rawToWordAlign);
    return inputToWordAlign;
  }

  public static TokenAlignment wordToWordAlignment(List<String> a, List<String> b) {
    if (a.size() == 0 || b.size() == 0) {
      return null;
    }
    WerAlignmentScorer scorer = new WerAlignmentScorer(a, b);
    scorer.setMaxTokensPerChunk(MAX_TOKENS_PER_CHUNK);
    TokenAlignment alignment = AlignmentGenerator.heuristicAlignment(scorer);
    if (alignment == null) {
      alignment = backoffAlignment(a, b);
    }
    return alignment;
  }
}
