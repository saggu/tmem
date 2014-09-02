package edu.isi.madcat.tmem.processors;

import java.util.regex.Pattern;

import org.springframework.beans.factory.InitializingBean;

public class PatternProcessor implements InitializingBean {
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  private Pattern pattern;

  private StringTransformer transformer;

  private String transformerName;

  private String transformerParams;

  private String id;
  
  private static int nextId = 0;
  
  public PatternProcessor() {
    super();
    this.pattern = null;
    this.transformer = null;
    this.transformerName = null;
    this.transformerParams = null;
    this.id = null;
  }

  public void afterPropertiesSet() throws Exception {
    String transformerParams = "";
    if (this.transformerParams != null) {
      transformerParams = this.transformerParams;
    }
    this.transformer = StringTransformerFactory.create(transformerName, transformerParams);
    if (this.id == null) {
      this.id = "P_"+PatternProcessor.nextId;
    }
    PatternProcessor.nextId++;
  }

  public Pattern getPattern() {
    return pattern;
  }

  public StringTransformer getTransformer() {
    return transformer;
  }

  public String getTransformerName() {
    return transformerName;
  }

  public String getTransformerParams() {
    return transformerParams;
  }

  public void setPattern(Pattern pattern) {
    this.pattern = pattern;
  }

  public void setPattern(String pattern) {
    this.pattern = Pattern.compile(pattern);
  }
  public void setTransformer(StringTransformer transformer) {
    this.transformer = transformer;
  }
  public void setTransformerName(String transformerName) {
    this.transformerName = transformerName;
  }
  public void setTransformerParams(String transformerParams) {
    this.transformerParams = transformerParams;
  }

  @Override
  public String toString() {
    return "PatternProcessor [pattern=" + pattern + ", transformer=" + transformer
        + ", transformerName=" + transformerName + ", transformerParams=" + transformerParams + "]";
  }

}
