package edu.isi.madcat.tmem.training.hmmaligner;

import edu.isi.madcat.tmem.utils.ParameterMap;

public class HmmConfig {

  private int maxSentLength;

  private int numIterations;

  private String sourceField;

  private String targetField;

  private String alignmentField;

  private String[] corpusFiles;

  private String outputFile;

  public HmmConfig(ParameterMap params) {
    initialize(params);
  }

  public HmmConfig(String parameterFile) {
    ParameterMap params = new ParameterMap(parameterFile);
    initialize(params);
  }

  public String getAlignmentField() {
    return alignmentField;
  }

  public String[] getCorpusFiles() {
    return corpusFiles;
  }

  public int getMaxSentLength() {
    return maxSentLength;
  }

  public int getNumIterations() {
    return numIterations;
  }

  public String getOutputFile() {
    return outputFile;
  }

  public String getSourceField() {
    return sourceField;
  }

  public String getTargetField() {
    return targetField;
  }

  public void setAlignmentField(String alignmentField) {
    this.alignmentField = alignmentField;
  }

  public void setCorpusFiles(String[] corpusFiles) {
    this.corpusFiles = corpusFiles;
  }

  public void setMaxSentLength(int maxSentLength) {
    this.maxSentLength = maxSentLength;
  }

  public void setNumIterations(int numIterations) {
    this.numIterations = numIterations;
  }

  public void setOutputFile(String outputFile) {
    this.outputFile = outputFile;
  }

  public void setSourceField(String sourceField) {
    this.sourceField = sourceField;
  }

  public void setTargetField(String targetField) {
    this.targetField = targetField;
  }

  private void initialize(ParameterMap params) {
    maxSentLength = 120;
    numIterations = 10;
    corpusFiles = null;

    if (params.hasParam("maxSentLength")) {
      maxSentLength = params.getIntRequired("max_sent_length");
    }

    if (params.hasParam("numIterations")) {
      numIterations = params.getIntRequired("num_iterations");
    }

    sourceField = params.getStringRequired("source_field");
    targetField = params.getStringRequired("target_field");
    alignmentField = params.getStringRequired("alignment_field");
    corpusFiles = params.getStringArrayRequired("corpus_files");
    outputFile = params.getStringRequired("output_file");
  }
}
