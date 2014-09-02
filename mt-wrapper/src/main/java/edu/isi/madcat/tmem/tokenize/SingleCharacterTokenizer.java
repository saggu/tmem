package edu.isi.madcat.tmem.tokenize;

import java.util.ArrayList;
import java.util.List;

import edu.isi.madcat.tmem.alignment.TokenAlignment;

public class SingleCharacterTokenizer extends Tokenizer {

  public static List<String> tokenizeStatic(String input, TokenAlignment alignment) {
    List<String> output = new ArrayList<String>();
    for (int i = 0; i < input.length(); i++) {
      String str = new String(input.substring(i, i + 1));
      output.add(str);
      if (alignment != null) {
        alignment.add(i, i, i, i);
      }
    }
    return output;
  }
  
  @Override
  public List<String> tokenize(String input, TokenAlignment alignment) {
    return SingleCharacterTokenizer.tokenizeStatic(input, alignment);
  }
}
