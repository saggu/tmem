package edu.isi.madcat.tmem.tokenize;

import edu.isi.madcat.tmem.utils.ParameterMap;

public class TokenizerFactory {
  public static Tokenizer create(String name) {
    return create(name, null);
  }

  public static Tokenizer create(String name, String tokenizerParamFile) {
    Tokenizer tokenizer = null;
    if (name.equals("whitespace")) {
      tokenizer = CharacterTokenizer.WHITESPACE;
    } else if (name.equals("chinese_unigram")) {
      tokenizer = new ChineseUnigramTokenizer();
    } else if (name.equals("english")) {
      tokenizer = new EnglishTokenizer();
    } else if (name.equals("korean")) {
      tokenizer = new KoreanTokenizer();
    } else if (name.equals("rev_korean")) {
      tokenizer = new ReversableKoreanTokenizer();
    } else if (name.equals("single_character")) {
      return new SingleCharacterTokenizer();
    } else {
      throw new RuntimeException("Unknown tokenizer: " + name);
    }
    ParameterMap params = null;
    if (tokenizerParamFile != null && !tokenizerParamFile.equals("")) {
      params = new ParameterMap(tokenizerParamFile);
    }
    tokenizer.initialize(params);
    return tokenizer;
  }

  public static ReversableTokenizer createReversable(String name, String tokenizerParamFile) {
    ReversableTokenizer tokenizer = null;
    if (name.equals("rev_korean")) {
      tokenizer = new ReversableKoreanTokenizer();
    } else {
      throw new RuntimeException("Unknown tokenizer: " + name);
    }
    if (tokenizerParamFile != null && !tokenizerParamFile.equals("")) {
      ParameterMap params = new ParameterMap(tokenizerParamFile);
      tokenizer.initialize(params);
    }
    return tokenizer;
  }
}
