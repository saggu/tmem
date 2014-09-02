package edu.isi.madcat.tmem.lookup.api;

import edu.isi.madcat.tmem.lookup.CorpusTerm;

public class LookupQuery {
  private String lookupKey;
  private boolean isBilingual;
  private int dictionaryId;

  public LookupQuery() {

  }

  public LookupQuery(String lookupKey) {
    super();
    this.lookupKey = lookupKey;
    this.isBilingual = false;
    this.dictionaryId = -1;
  }

  public LookupQuery(String source, String target) {
    super();
    this.lookupKey = CorpusTerm.getJointString(source, target);
    this.isBilingual = true;
    this.dictionaryId = -1;
  }

  public LookupQuery(int dictionaryId) {
    super();
    this.lookupKey = null;
    this.isBilingual = false;
    this.dictionaryId = dictionaryId;
  }

  public String getLookupKey() {
    return lookupKey;
  }

  public void setLookupKey(String lookupKey) {
    this.lookupKey = lookupKey;
  }

  public int getDictionaryId() {
    return dictionaryId;
  }

  public void setDictionaryId(int dictionaryId) {
    this.dictionaryId = dictionaryId;
  }

  public boolean isBilingual() {
    return isBilingual;
  }

  public void setBilingual(boolean isBilingual) {
    this.isBilingual = isBilingual;
  }
}
