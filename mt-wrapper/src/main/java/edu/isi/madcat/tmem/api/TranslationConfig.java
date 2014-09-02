package edu.isi.madcat.tmem.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;

import edu.isi.madcat.tmem.logging.ExceptionHandler;

public class TranslationConfig {
  public String getSqlConfigFile() {
    return sqlConfigFile;
  }

  public void setSqlConfigFile(String sqlConfigFile) {
    this.sqlConfigFile = sqlConfigFile;
  }

  private String sourceLanguage;

  private String targetLanguage;

  private int requestAltNbestSize;

  private int printAltNbestSize;

  private int maxPhraseDupe;

  private String targetFunctionWordFile;

  private Set<String> targetFunctionWords;

  private String sourceTokenizerName;

  private String sourceTokenizerConfig;

  private String sqlConfigFile;
  
  public TranslationConfig() {

  }

  public int getMaxPhraseDupe() {
    return maxPhraseDupe;
  }

  public int getPrintAltNbestSize() {
    return printAltNbestSize;
  }

  public int getRequestAltNbestSize() {
    return requestAltNbestSize;
  }

  public String getSourceLanguage() {
    return sourceLanguage;
  }
  
  public String getSourceTokenizerConfig() {
    return sourceTokenizerConfig;
  }
  
  
  public String getSourceTokenizerName() {
    return sourceTokenizerName;
  }

  public String getTargetFunctionWordFile() {
    return targetFunctionWordFile;
  }

  public String getTargetLanguage() {
    return targetLanguage;
  }

  public void setMaxPhraseDupe(int maxPhraseDupe) {
    this.maxPhraseDupe = maxPhraseDupe;
  }

  public void setPrintAltNbestSize(int printAltNbestSize) {
    this.printAltNbestSize = printAltNbestSize;
  }

  public void setRequestAltNbestSize(int requestAltNbestSize) {
    this.requestAltNbestSize = requestAltNbestSize;
  }

  public void setSourceLanguage(String sourceLanguage) {
    this.sourceLanguage = sourceLanguage;
  }

  public void setSourceTokenizerConfig(String sourceTokenizerConfig) {
    this.sourceTokenizerConfig = sourceTokenizerConfig;
  }

  public void setSourceTokenizerName(String sourceTokenizerName) {
    this.sourceTokenizerName = sourceTokenizerName;
  }

  public void setTargetFunctionWordFile(String targetFunctionWordFile) {
    this.targetFunctionWordFile = targetFunctionWordFile;
  }

  public void setTargetFunctionWords(Set<String> targetFunctionWords) {
    this.targetFunctionWords = targetFunctionWords;
  }

  public void setTargetLanguage(String targetLanguage) {
    this.targetLanguage = targetLanguage;
  }

  Set<String> getTargetFunctionWords() {
    if (targetFunctionWords == null) {
      return null;
    }
    if (targetFunctionWords == null) {
      targetFunctionWords = new HashSet<String>();
      InputStream is = getClass().getResourceAsStream(targetFunctionWordFile);
      List<String> lines = null;
      try {
        lines = IOUtils.readLines(is, "utf-8");
      } catch (IOException e) {
        ExceptionHandler.handle(e);
      }
      targetFunctionWords = new HashSet<String>();
      for (String line : lines) {
        targetFunctionWords.add(line);
      }
    }
    return targetFunctionWords;
  }

}