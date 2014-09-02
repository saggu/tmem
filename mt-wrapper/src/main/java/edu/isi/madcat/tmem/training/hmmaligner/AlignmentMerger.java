package edu.isi.madcat.tmem.training.hmmaligner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

public class AlignmentMerger {

  enum Algorithm {
    INTERSECT, GROW_DIAG_FINAL
  };

  Algorithm algorithm;

  public List<Pair<Integer, Integer>> getIntersection(List<Integer> fw, List<Integer> bw) {
    List<Pair<Integer, Integer>> links = new ArrayList<Pair<Integer, Integer>>();
    Map<Integer, Integer> bwMap = new HashMap<Integer, Integer>();
    for (int i = 0; i < bw.size(); i++) {
      bwMap.put(bw.get(i), i);
    }
    for (int i = 0; i < fw.size(); i++) {
      Integer bwIndex = bwMap.get(i);
      if (bwIndex != null) {
        links.add(new ImmutablePair<Integer, Integer>(i, bwIndex));
      }
    }
    return links;
  }

  public List<Pair<Integer, Integer>> mergeAlignment(List<Integer> fw, List<Integer> bw) {
    List<Pair<Integer, Integer>> links = new ArrayList<Pair<Integer, Integer>>();
    if (algorithm == Algorithm.INTERSECT) {
      links = getIntersection(fw, bw);
    } else if (algorithm == Algorithm.GROW_DIAG_FINAL) {

    }
    return links;
  }
}
