package edu.isi.madcat.tmem.processors;

import java.util.regex.Pattern;

public class SelfTransformer extends StringTransformer {

  private static Pattern numberPattern;

  static {
    numberPattern = Pattern.compile("[0-9]");
  }

  @Override
  public void setParams(String params) {
  }

  @Override
  public TransformerOutput transformString(String input, PatternMatcher matcher) {
    String type = "default";
    if (numberPattern.matcher(input).find()) {
      type = "number";
    }
    return new TransformerOutput(input, type);
  }
}
