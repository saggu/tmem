package edu.isi.madcat.tmem.tokenize;

import java.util.List;

import edu.isi.madcat.tmem.alignment.TokenAlignment;
import edu.isi.madcat.tmem.utils.ParameterMap;

public abstract class Tokenizer {
  public List<String> tokenize(String input) {
    return tokenize(input, null);
  }

  public void initialize(ParameterMap params) {
    
  }
  
  public abstract List<String> tokenize(String input, TokenAlignment alignment);
  
  public boolean isLanguageChar(char c) {
    return false;
  }
}
