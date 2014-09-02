package edu.isi.madcat.tmem.backend.messages;

import java.util.List;

import edu.isi.madcat.tmem.serialization.SimpleSerializable;

public class TranslationTree extends SimpleSerializable {
  protected List<TranslationTreeNode> nodes;

  protected Long rootIndex;

  public TranslationTree() {

  }

  public TranslationTree(List<TranslationTreeNode> nodes, Long rootIndex) {
    super();
    this.nodes = nodes;
    this.rootIndex = rootIndex;
  }

  public List<TranslationTreeNode> getNodes() {
    return nodes;
  }

  public Long getRootIndex() {
    return rootIndex;
  }

  public void setNodes(List<TranslationTreeNode> nodes) {
    this.nodes = nodes;
  }

  public void setRootIndex(Long rootIndex) {
    this.rootIndex = rootIndex;
  }
}
