package edu.isi.madcat.tmem.backend.messages;

import java.util.List;

import edu.isi.madcat.tmem.serialization.SimpleSerializable;

public class TranslationResponse extends SimpleSerializable {
  protected List<TranslationHypothesis> hypotheses;

  protected TranslationTree tree;

  public TranslationResponse() {

  }

  public List<TranslationHypothesis> getHypotheses() {
    return hypotheses;
  }

  public TranslationTree getTree() {
    return tree;
  }

  public void setHypotheses(List<TranslationHypothesis> hypotheses) {
    this.hypotheses = hypotheses;
  }

  public void setTree(TranslationTree tree) {
    this.tree = tree;
  }

  @Override
  public String toString() {
    return "TranslationResponse [hypotheses=" + hypotheses + ", tree=" + tree + "]";
  }

}
