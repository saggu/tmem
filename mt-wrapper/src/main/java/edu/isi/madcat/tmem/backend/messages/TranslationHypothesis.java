package edu.isi.madcat.tmem.backend.messages;

import java.util.List;

import edu.isi.madcat.tmem.serialization.SimpleSerializable;

public class TranslationHypothesis extends SimpleSerializable {
  protected List<String> detokenizedWords;

  protected String id;

  protected Double totalScore;

  protected List<String> translatedWords;

  protected List<WordAlignment> wordAlignment;

  public TranslationHypothesis() {

  }

  public TranslationHypothesis(String id, List<String> translatedWords,
      List<String> detokenizedWords, List<WordAlignment> wordAlignment, Double totalScore) {
    super();
    this.id = id;
    this.translatedWords = translatedWords;
    this.detokenizedWords = detokenizedWords;
    this.wordAlignment = wordAlignment;
    this.totalScore = totalScore;
  }

  public List<String> getDetokenizedWords() {
    return detokenizedWords;
  }

  public String getId() {
    return id;
  }

  public Double getTotalScore() {
    return totalScore;
  }

  public List<String> getTranslatedWords() {
    return translatedWords;
  }

  public List<WordAlignment> getWordAlignment() {
    return wordAlignment;
  }

  @Override
  public String toString() {
    return "TranslationHypothesis [detokenizedWords=" + detokenizedWords + ", id=" + id
        + ", totalScore=" + totalScore + ", translatedWords=" + translatedWords
        + ", wordAlignment=" + wordAlignment + "]";
  }
}
