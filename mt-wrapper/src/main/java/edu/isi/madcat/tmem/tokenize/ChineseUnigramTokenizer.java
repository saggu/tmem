package edu.isi.madcat.tmem.tokenize;

import java.util.ArrayList;
import java.util.List;

import edu.isi.madcat.tmem.alignment.TokenAlignment;

public class ChineseUnigramTokenizer extends Tokenizer {

  @Override
  public List<String> tokenize(String input, TokenAlignment alignment) {
    int inputStart = 0;
    int inputEnd = 0;
    StringBuilder currentToken = null;
    List<String> output = new ArrayList<String>();
    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);
      boolean doAddToken = false;
      boolean doOutput = true;
      if (CharacterTokenizer.WHITESPACE.isChar(c)) {
        doAddToken = true;
        doOutput = false;
      } else if (!isAscii(c)) {
        doAddToken = true;
        doOutput = true;
      }

      if (doAddToken) {
        if (currentToken != null) {
          if (alignment != null) {
            alignment.add(inputStart, inputEnd, output.size(), output.size());
          }
          output.add(currentToken.toString());
          currentToken = null;
        }
      }

      if (doOutput) {
        if (currentToken == null) {
          currentToken = new StringBuilder();
          inputStart = i;
        }
        currentToken.append(c);
        inputEnd = i;
      }

      if (doAddToken) {
        if (currentToken != null) {
          if (alignment != null) {
            alignment.add(inputStart, inputEnd, output.size(), output.size());
          }
          output.add(currentToken.toString());
          currentToken = null;
        }
      }
    }
    if (currentToken != null) {
      if (alignment != null) {
        alignment.add(inputStart, inputEnd, output.size(), output.size());
      }
      output.add(currentToken.toString());
    }
    return output;
  }

  private boolean isAscii(char c) {
    return (c < 128);
  }

}
