package edu.isi.madcat.tmem.lextrans;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

public class TranslationRule implements java.io.Serializable {
  @Override
  public String toString() {
    return "TranslationRule [sourceWords=" + sourceWords + ", targetWords=" + targetWords
        + ", alignment=" + alignment + "]";
  }

  static final long serialVersionUID = 1L;

  private List<String> sourceWords;

  private List<String> targetWords;

  private List<Pair<Integer, Integer>> alignment;

  private String key;

  public TranslationRule() {

  }

  public TranslationRule(List<String> sourceWords, List<String> targetWords,
      List<Pair<Integer, Integer>> alignment) {
    super();
    this.sourceWords = sourceWords;
    this.targetWords = targetWords;
    this.alignment = alignment;
    initialize();
  }

  public List<Pair<Integer, Integer>> getAlignment() {
    return alignment;
  }

  public String getKey() {
    return key;
  }

  public List<String> getSourceWords() {
    return sourceWords;
  }

  public List<String> getTargetWords() {
    return targetWords;
  }

  public void initialize() {
    key = createKey(sourceWords);
  }

  public static String createKey(List<String> sourceWords) {
    return StringUtils.join(sourceWords, " ");
  }
  
  public void setAlignment(List<Pair<Integer, Integer>> alignment) {
    this.alignment = alignment;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public void setSourceWords(List<String> sourceWords) {
    this.sourceWords = sourceWords;
  }

  public void setTargetWords(List<String> targetWords) {
    this.targetWords = targetWords;
  }
}
