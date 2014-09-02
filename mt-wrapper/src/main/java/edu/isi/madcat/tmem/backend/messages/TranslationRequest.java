package edu.isi.madcat.tmem.backend.messages;

import java.util.List;

import edu.isi.madcat.tmem.serialization.SimpleSerializable;

public class TranslationRequest extends SimpleSerializable {
  protected List<AuxiliaryRule> auxiliaryRules;

  protected List<BiasedTranslation> biasedTranslations;

  protected Boolean doOutputTree;

  protected Long nbestSize;

  protected Boolean partialTranslation;

  protected List<String> sourceWords;

  public TranslationRequest() {

  }

  public TranslationRequest(List<String> sourceWords, List<AuxiliaryRule> auxiliaryRules,
      List<BiasedTranslation> forcedTranslations, Boolean partialTranslation, Long nbestSize) {
    super();
    this.sourceWords = sourceWords;
    this.auxiliaryRules = auxiliaryRules;
    this.biasedTranslations = forcedTranslations;
    this.partialTranslation = partialTranslation;
    this.nbestSize = nbestSize;
    this.doOutputTree = false;
  }

  public List<AuxiliaryRule> getAuxiliaryRules() {
    return auxiliaryRules;
  }

  public List<BiasedTranslation> getBiasedTranslations() {
    return biasedTranslations;
  }

  public Boolean getDoOutputTree() {
    return doOutputTree;
  }

  public List<BiasedTranslation> getForcedTranslations() {
    return biasedTranslations;
  }

  public Long getNbestSize() {
    return nbestSize;
  }

  public Boolean getPartialTranslation() {
    return partialTranslation;
  }

  public List<String> getSourceWords() {
    return sourceWords;
  }

  public void setAuxiliaryRules(List<AuxiliaryRule> auxiliaryRules) {
    this.auxiliaryRules = auxiliaryRules;
  }

  public void setBiasedTranslations(List<BiasedTranslation> biasedTranslations) {
    this.biasedTranslations = biasedTranslations;
  }

  public void setDoOutputTree(Boolean doOutputTree) {
    this.doOutputTree = doOutputTree;
  }

  public void setNbestSize(Long nbestSize) {
    this.nbestSize = nbestSize;
  }

  public void setPartialTranslation(Boolean partialTranslation) {
    this.partialTranslation = partialTranslation;
  }

  public void setSourceWords(List<String> sourceWords) {
    this.sourceWords = sourceWords;
  }

}
