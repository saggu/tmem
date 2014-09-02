package edu.isi.madcat.tmem.tokenize;

import java.util.ArrayList;
import java.util.List;

import edu.isi.madcat.tmem.alignment.AlignmentPair;
import edu.isi.madcat.tmem.alignment.Range;
import edu.isi.madcat.tmem.alignment.TokenAlignment;

public class CharacterTokenizer extends Tokenizer {
  public final static CharacterTokenizer WHITESPACE;

  static {
    List<Character> whiteSpaceChars = new ArrayList<Character>();
    whiteSpaceChars.add(new Character(' '));
    whiteSpaceChars.add(new Character('\t'));
    WHITESPACE = new CharacterTokenizer(whiteSpaceChars);
  }

  protected char[] chars;

  public CharacterTokenizer(char c) {
    chars = new char[1];
    chars[0] = c;
  }

  public CharacterTokenizer(List<Character> charList) {
    chars = new char[charList.size()];
    for (int i = 0; i < charList.size(); i++) {
      chars[i] = charList.get(i).charValue();
    }
  }

  public boolean isChar(char c) {
    for (char x : chars) {
      if (x == c) {
        return true;
      }
    }
    return false;
  }

  @Override
  public List<String> tokenize(String input) {
    return tokenize(input, null);
  }

  @Override
  public List<String> tokenize(String input, TokenAlignment alignment) {
    StringBuilder currentToken = null;
    List<String> tokens = new ArrayList<String>();
    int tokenStart = 0;
    int tokenEnd = 0;
    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);
      if (isChar(c)) {
        if (currentToken != null) {
          addToken(tokens, currentToken, tokenStart, tokenEnd, alignment);
          currentToken = null;
        }
      } else {
        if (currentToken == null) {
          currentToken = new StringBuilder();
          tokenStart = i;
        }
        currentToken.append(c);
        tokenEnd = i;
      }
    }
    if (currentToken != null) {
      addToken(tokens, currentToken, tokenStart, tokenEnd, alignment);
    }
    return tokens;
  }

  protected void addToken(List<String> tokens, StringBuilder currentToken, int tokenStart,
      int tokenEnd, TokenAlignment alignment) {
    int index = tokens.size();
    tokens.add(currentToken.toString());
    if (alignment != null) {
      alignment.add(new AlignmentPair(new Range(tokenStart, tokenEnd), new Range(index, index)));
    }
  }


}
