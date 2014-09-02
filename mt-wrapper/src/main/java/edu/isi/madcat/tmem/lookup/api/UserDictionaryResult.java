package edu.isi.madcat.tmem.lookup.api;

import java.util.List;

public class UserDictionaryResult {
  private String lookupKey;

  private int dictionaryId;

  private int userId;
  
  private int groupId;

  private String sourceTermRaw;

  private String sourceTermTok;

  private String sourceAcronymRaw;

  private String sourceAcronymTok;

  private String targetTermRaw;

  private String targetTermTok;

  private String targetAcronymRaw;

  private String targetAcronymTok;

  private boolean doesMatchSource;

  private int matchScore;

  private int corpusCount;

  public UserDictionaryResult(List<String> rs) {
    super();
    this.lookupKey = rs.get(0);
    this.dictionaryId = Integer.parseInt(rs.get(1));
    this.userId = Integer.parseInt(rs.get(2));
    this.groupId = Integer.parseInt(rs.get(3));
    this.sourceTermRaw = rs.get(4);
    this.sourceTermTok = rs.get(5);
    this.sourceAcronymRaw = rs.get(6);
    this.sourceAcronymTok = rs.get(7);
    this.targetTermRaw = rs.get(8);
    this.targetTermTok = rs.get(9);
    this.targetAcronymRaw = rs.get(10);
    this.targetAcronymTok = rs.get(11);
    this.doesMatchSource = (Integer.parseInt(rs.get(12)) > 0) ? true : false;
    this.matchScore = Integer.parseInt(rs.get(13));
  }

  public int getCorpusCount() {
    return corpusCount;
  }

  public int getDictionaryId() {
    return dictionaryId;
  }

  public int getGroupId() {
    return groupId;
  }

  public String getLookupKey() {
    return lookupKey;
  }

  public int getMatchScore() {
    return matchScore;
  }

  public String getSourceAcronymRaw() {
    return sourceAcronymRaw;
  }

  public String getSourceAcronymTok() {
    return sourceAcronymTok;
  }

  public String getSourceTermRaw() {
    return sourceTermRaw;
  }

  public String getSourceTermTok() {
    return sourceTermTok;
  }

  public String getTargetAcronymRaw() {
    return targetAcronymRaw;
  }

  public String getTargetAcronymTok() {
    return targetAcronymTok;
  }

  public String getTargetTermRaw() {
    return targetTermRaw;
  }

  public String getTargetTermTok() {
    return targetTermTok;
  }

  public int getUserId() {
    return userId;
  }

  public boolean isDoesMatchSource() {
    return doesMatchSource;
  }

  public void setCorpusCount(int corpusCount) {
    this.corpusCount = corpusCount;
  }

  public void setDictionaryId(int dictionaryId) {
    this.dictionaryId = dictionaryId;
  }

  public void setDoesMatchSource(boolean doesMatchSource) {
    this.doesMatchSource = doesMatchSource;
  }

  public void setGroupId(int groupId) {
    this.groupId = groupId;
  }

  public void setLookupKey(String lookupKey) {
    this.lookupKey = lookupKey;
  }

  public void setMatchScore(int matchScore) {
    this.matchScore = matchScore;
  }

  public void setSourceAcronymRaw(String sourceAcronymRaw) {
    this.sourceAcronymRaw = sourceAcronymRaw;
  }

  public void setSourceAcronymTok(String sourceAcronymTok) {
    this.sourceAcronymTok = sourceAcronymTok;
  }

  public void setSourceTermRaw(String sourceTermRaw) {
    this.sourceTermRaw = sourceTermRaw;
  }

  public void setSourceTermTok(String sourceTermTok) {
    this.sourceTermTok = sourceTermTok;
  }

  public void setTargetAcronymRaw(String targetAcronymRaw) {
    this.targetAcronymRaw = targetAcronymRaw;
  }

  public void setTargetAcronymTok(String targetAcronymTok) {
    this.targetAcronymTok = targetAcronymTok;
  }

  public void setTargetTermRaw(String targetTermRaw) {
    this.targetTermRaw = targetTermRaw;
  }

  public void setTargetTermTok(String targetTermTok) {
    this.targetTermTok = targetTermTok;
  }

  public void setUserId(int userId) {
    this.userId = userId;
  }

}
