package edu.isi.madcat.tmem.processors;

import java.util.ArrayList;
import java.util.List;

public class HierarchicalOutput {
  public enum OutputType {
    JOIN, TRANSLATE
  }

  public class OutputValue {
    public String getJoinType() {
      return joinType;
    }

    public void setJoinType(String joinType) {
      this.joinType = joinType;
    }

    private OutputType type;
    private String text;
    private String joinType;
    public OutputValue(OutputType type, String text, String joinType) {
      
      super();
      this.type = type;
      this.text = text;
      this.joinType = joinType;
    }

    public String getText() {
      return text;
    }

    public OutputType getType() {
      return type;
    }

    public void setText(String text) {
      this.text = text;
    }

    public void setType(OutputType type) {
      this.type = type;
    }
  }

  private List<OutputValue> values;

  public HierarchicalOutput() {
    super();
    values = new ArrayList<OutputValue>();
  }

  public HierarchicalOutput(List<OutputValue> values) {
    super();
    this.values = values;
  }

  public List<OutputValue> getValues() {
    return values;
  }

  public void setValues(List<OutputValue> values) {
    this.values = values;
  }

  public void add(String typeId, String text, String joinType) {
    OutputType type = parseTypeId(typeId);

    values.add(new OutputValue(type, text, joinType));
  }

  private OutputType parseTypeId(String typeId) {
    if (typeId.equals("J")) {
      return OutputType.JOIN;
    } else if (typeId.equals("T")) {
      return OutputType.TRANSLATE;
    } else {
      throw new RuntimeException("Bad type id: " + typeId);
    }
  }
}
