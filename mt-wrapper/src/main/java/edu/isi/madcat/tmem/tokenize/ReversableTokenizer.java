package edu.isi.madcat.tmem.tokenize;

import java.util.List;

import edu.isi.madcat.tmem.alignment.TokenAlignment;

public abstract class ReversableTokenizer extends Tokenizer {

  public abstract List<String> tokenize(String input, TokenAlignment alignment, List<String> rawTokens, List<Integer> splitAlignment);

}
