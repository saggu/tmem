package edu.isi.madcat.tmem.lookup.api;

import java.util.ArrayList;
import java.util.List;

public class LookupQuerySet {

  private List<LookupQuery> queries;

  public List<LookupQuery> getQueries() {
    return queries;
  }

  public void setQueries(List<LookupQuery> queries) {
    this.queries = queries;
  }

  public LookupQuerySet() {
    this.queries = new ArrayList<LookupQuery>();
  }
  
  public LookupQuerySet(List<LookupQuery> queries) {
    super();
    this.queries = queries;
  }

  public void addQuery(LookupQuery lookupQuery) {
    queries.add(lookupQuery);
  }
}
