package edu.isi.madcat.tmem.lookup.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nocrala.tools.texttablefmt.BorderStyle;
import org.nocrala.tools.texttablefmt.Table;

public class DictionaryResultSet {

  private List<DictionaryResult> results;

  public DictionaryResultSet() {
    results = new ArrayList<DictionaryResult>();
  }

  public void addResult(DictionaryResult dictionaryResult) {
    results.add(dictionaryResult);
  }

  public List<DictionaryResult> getResults() {
    return results;
  }

  public void setResults(List<DictionaryResult> results) {
    this.results = results;
  }

  public void sortAndFilterResults() {
    Collections.sort(results, new Comparator<DictionaryResult>() {
      public int compare(DictionaryResult r1, DictionaryResult r2) {
        int c = 0;
        c = r1.getMatchScore() - r2.getMatchScore();
        if (c != 0) {
          return c;
        }
        c = r2.getCorpusCount() - r1.getCorpusCount(); // the one with the greater corpus count should be first
        if (c != 0) {
          return c;
        }
        c = r1.getTargetTermRaw().compareTo(r2.getTargetTermRaw());
        if (c != 0) {
          return c;
        }
        c = r1.getTargetAcronymRaw().compareTo(r2.getTargetAcronymRaw());
        if (c != 0) {
          return c;
        }
        c = r1.getSourceTermRaw().compareTo(r2.getSourceTermRaw());
        if (c != 0) {
          return c;
        }
        c = r1.getSourceAcronymRaw().compareTo(r2.getSourceAcronymRaw());
        if (c != 0) {
          return c;
        }
        return 0;
      }
    });

    Map<Integer, DictionaryResult> seenItems = new HashMap<Integer, DictionaryResult>();
    List<DictionaryResult> filteredResults = new ArrayList<DictionaryResult>();
    for (DictionaryResult res : results) {
      if (!seenItems.containsKey(res.getDictionaryId())) {
        filteredResults.add(res);
        seenItems.put(res.getDictionaryId(), res);
      }
    }
    results = filteredResults;
  }

  public String toTextTable() {
    Table t = new Table(7, BorderStyle.CLASSIC_WIDE);
    t.addCell("Entry ID");
    t.addCell("English Acronym");
    t.addCell("English Term");
    t.addCell("Korean Acronym");
    t.addCell("Korean Term");
    t.addCell("Collection");
    t.addCell("Corpus Count");
    for (DictionaryResult result : results) {
      t.addCell(""+result.getDictionaryId());
      t.addCell(result.getTargetAcronymRaw());
      t.addCell(result.getTargetTermRaw());
      t.addCell(result.getSourceAcronymRaw());
      t.addCell(result.getSourceTermRaw());
      t.addCell(result.getCollection().getFullName());
      t.addCell(""+result.getCorpusCount());
    }
    return t.render();
  }
}
