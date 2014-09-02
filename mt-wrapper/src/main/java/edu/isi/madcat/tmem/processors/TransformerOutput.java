package edu.isi.madcat.tmem.processors;

public class TransformerOutput {
  public String getOutput() {
    return output;
  }

  public void setOutput(String output) {
    this.output = output;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  private String output;
  private String type;

  public TransformerOutput(String output, String type) {
    super();
    this.output = output;
    this.type = type;
  }

}
