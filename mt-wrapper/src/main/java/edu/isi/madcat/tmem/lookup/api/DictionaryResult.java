package edu.isi.madcat.tmem.lookup.api;

import java.sql.ResultSet;
import java.sql.SQLException;

import edu.isi.madcat.tmem.logging.ExceptionHandler;
import edu.isi.madcat.tmem.sql.SqlUtils;

public class DictionaryResult {

  private int dictionaryId;

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
  
  private CollectionInfo collection;

  public DictionaryResult() {
    
  }
  
  public DictionaryResult(ResultSet rs) {
    super();
    try {
      this.dictionaryId = rs.getInt(2);
      this.sourceTermRaw = SqlUtils.getString(rs, 3);
      this.sourceTermTok = SqlUtils.getString(rs, 4);
      this.sourceAcronymRaw = SqlUtils.getString(rs, 5);
      this.sourceAcronymTok = SqlUtils.getString(rs, 6);
      this.targetTermRaw = SqlUtils.getString(rs, 7);
      this.targetTermTok = SqlUtils.getString(rs, 8);
      this.targetAcronymRaw = SqlUtils.getString(rs, 9);
      this.targetAcronymTok = SqlUtils.getString(rs, 10);
      this.doesMatchSource = (rs.getInt(11) > 0) ? true : false;
      this.matchScore = rs.getInt(12);
      this.collection =
          new CollectionInfo(rs.getInt(13), SqlUtils.getString(rs, 14), SqlUtils.getString(rs, 15));
      this.corpusCount = rs.getInt(16);
    } catch (SQLException e) {
      ExceptionHandler.handle(e);
    }
  }

  public CollectionInfo getCollection() {
    return collection;
  }

  public int getCorpusCount() {
    return corpusCount;
  }

  public int getDictionaryId() {
    return dictionaryId;
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

  public boolean isDoesMatchSource() {
    return doesMatchSource;
  }

  public void setCollection(CollectionInfo collection) {
    this.collection = collection;
  }

  public void setCorpusCount(int corpusCount) {
    this.corpusCount = corpusCount;
  }

  public void setDoesMatchSource(boolean doesMatchSource) {
    this.doesMatchSource = doesMatchSource;
  }

  public void setDictionaryId(int dictionaryId) {
    this.dictionaryId = dictionaryId;
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

  public static DictionaryResult fromUserDictResultSet(ResultSet rs) {
    DictionaryResult dictRes = new DictionaryResult();
    try {
      dictRes.dictionaryId = rs.getInt(2);
      dictRes.sourceTermRaw = SqlUtils.getString(rs, 3);
      dictRes.sourceTermTok = SqlUtils.getString(rs, 4);
      dictRes.sourceAcronymRaw = SqlUtils.getString(rs, 5);
      dictRes.sourceAcronymTok = SqlUtils.getString(rs, 6);
      dictRes.targetTermRaw = SqlUtils.getString(rs, 7);
      dictRes.targetTermTok = SqlUtils.getString(rs, 8);
      dictRes.targetAcronymRaw = SqlUtils.getString(rs, 9);
      dictRes.targetAcronymTok = SqlUtils.getString(rs, 10);
      dictRes.doesMatchSource = (rs.getInt(11) > 0) ? true : false;
      dictRes.matchScore = rs.getInt(12);
      dictRes.collection =
          new CollectionInfo(CollectionInfo.TYPE_USER, "user_dict", "User Dictionary");
      dictRes.corpusCount = 0;
    } catch (SQLException e) {
      ExceptionHandler.handle(e); 
    }
    return dictRes;
  }

}
