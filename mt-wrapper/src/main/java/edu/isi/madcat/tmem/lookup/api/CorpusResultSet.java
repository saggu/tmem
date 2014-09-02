package edu.isi.madcat.tmem.lookup.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.isi.madcat.tmem.alignment.Range;

public class CorpusResultSet {

  private List<CorpusResult> results;

  public CorpusResultSet() {
    results = new ArrayList<CorpusResult>();
  }

  public void addResult(CorpusResult result) {
    results.add(result);
  }

  private Object getHighlightedText(String str, List<Range> ranges) {
    Set<Integer> startIndexes = new HashSet<Integer>();
    Set<Integer> endIndexes = new HashSet<Integer>();
    for (Range range : ranges) {
      startIndexes.add(range.getStart());
      endIndexes.add(range.getEnd());
    }
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < str.length(); i++) {
      if (startIndexes.contains(i)) {
        sb.append("*");
      }
      sb.append(str.charAt(i));
      if (endIndexes.contains(i)) {
        sb.append("*");
      }
    }
    return sb.toString();
  }

  public List<CorpusResult> getResults() {
    return results;
  }

  public void setResults(List<CorpusResult> results) {
    this.results = results;
  }

  public void sortAndFilterResults() {
    Collections.sort(results, new Comparator<CorpusResult>() {
      public int compare(CorpusResult r1, CorpusResult r2) {
        return r1.getSegmentId() - r2.getSegmentId();
      }
    });

    Map<String, CorpusResult> seenItems = new HashMap<String, CorpusResult>();
    List<CorpusResult> filteredResults = new ArrayList<CorpusResult>();
    for (CorpusResult res : results) {
      String key = ""+res.getSegmentId()+"-"+res.getSourceRanges()+"-"+res.getTargetRanges();
      if (!seenItems.containsKey(key)) {
        filteredResults.add(res);
        seenItems.put(key, res);
      }
    }
    results = filteredResults;
  }

  public String toTextTable() {
    StringBuilder sb = new StringBuilder();
    for (CorpusResult result : results) {
      sb.append(getHighlightedText(result.getTarget(), result.getTargetRanges()));
      sb.append("\n");
      sb.append(getHighlightedText(result.getSource(), result.getSourceRanges()));
      sb.append("\n");
      sb.append("\n");
    }
    return sb.toString();
  }
}
