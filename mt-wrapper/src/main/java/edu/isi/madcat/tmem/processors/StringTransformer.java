package edu.isi.madcat.tmem.processors;

import java.util.List;

public abstract class StringTransformer {
  public abstract TransformerOutput transformString(String input, PatternMatcher matcher);
  
  public abstract void setParams(String params);
  
  public boolean hasMatcher() {
    return false;
  }
  
  public List<PatternMatcher> getMatchers(String input) {
    return null;
  }
}
