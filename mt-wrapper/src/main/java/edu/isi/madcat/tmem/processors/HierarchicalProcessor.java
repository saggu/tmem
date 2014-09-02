package edu.isi.madcat.tmem.processors;

import java.util.List;

public class HierarchicalProcessor {
  private List<HierarchicalTransformer> transformers;

  public HierarchicalProcessor() {
    super();
  }

  public List<HierarchicalTransformer> getTransformers() {
    return transformers;
  }

  public HierarchicalOutput processString(String input) {
    HierarchicalOutput output = null;
    for (int i = 0; i < transformers.size(); i++) {
      HierarchicalTransformer transformer = transformers.get(i);
      List<String> matchGroups = transformer.getMatchGroups(input);
      if (matchGroups != null) {
        output = new HierarchicalOutput();
        for (int j = 0; j < transformer.getIndexes().size(); j++) {
          int index = transformer.getIndexes().get(j).getIndex();
          String processType = transformer.getIndexes().get(j).getProcessType();
          String joinType = transformer.getIndexes().get(j).getJoinType();
          output.add(processType, matchGroups.get(index), joinType);
        }
        break;
      }
    }
    return output;
  }

  public void setTransformers(List<HierarchicalTransformer> transformers) {
    this.transformers = transformers;
  }
}
