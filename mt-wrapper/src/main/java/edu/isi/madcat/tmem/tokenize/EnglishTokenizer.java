package edu.isi.madcat.tmem.tokenize;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import edu.isi.madcat.tmem.alignment.AlignmentGenerator;
import edu.isi.madcat.tmem.alignment.AlignmentPair;
import edu.isi.madcat.tmem.alignment.Range;
import edu.isi.madcat.tmem.alignment.TokenAlignment;
import edu.isi.madcat.tmem.alignment.WerAlignmentScorer;
import edu.isi.madcat.tmem.utils.ParameterMap;
import edu.isi.madcat.tmem.utils.RegexReplacer;

public class EnglishTokenizer extends Tokenizer {
  private RegexReplacer transformer;
  private RegexReplacer splitter;
  private static Set<Character> characterSet = null;
  
  static  {
    characterSet = new HashSet<Character>();
    int start = 0x41; // 'A'
    int end = 0x7a; // 'z'
    for (int i = start; i <= end; i++) {
      characterSet.add((char)i);
    }
  }
  
  public EnglishTokenizer() {
    transformer = new RegexReplacer();

    transformer.addRegex("(\u201C|\u201D)", "\"");
    transformer.addRegex("(\u2018|2019)", "'");
    transformer.addRegex("(\u00B7)+", "\u00B7");

    splitter = new RegexReplacer();
    // character replacement
    splitter.addRegex("(\u201C|\u201D)", "\"");
    splitter.addRegex("(\u2018|2019)", "'");

    splitter.addRegex("(``)", "\"");
    splitter.addRegex("('')", "\"");
    splitter.addRegex("([,;@#$%&`])", " $1 ");
    splitter.addRegex("(['\"])", " $1 ");
    splitter.addRegex("([!?])", " $1 ");
    splitter.addRegex("(/\\s+/)", "//");
    splitter.addRegex("([\\]\\[(){}<>])", " $1 ");
    splitter.addRegex("'\\s+(s|ll|re|ve|t)( |$)", " '$1 ");
    splitter.addRegex("(\\.\\.\\.)", " ... ");
    splitter.addRegex("(\\.)", " . ");
    splitter.addRegex("(\\S)([:/-])(\\S)", "$1 @$2@ $3");
    splitter.addRegex("(\\s)([:/-])(\\S)", " $2@ $3");
    splitter.addRegex("(\\S)([:/-])(\\s)", "$1 @$2 ");
    splitter.addRegex("^(-)(\\S)", "$1@ $2");
    splitter.addRegex("([0-9])\\s+([,.])\\s+([0-9])", "$1$2$3");
    splitter.addRegex("(Mr|Mrs|Dr|Inc|Pfc|Sgt|Col|Lt|Gen|Maj|Spc|Capt|Cpl|Pvt|Jr|Mt)\\s+\\.", " $1. ");
    splitter.addRegex("([PpAa])\\s+\\.\\s+([Mm])\\s+\\.", " $1.$2. ");
    splitter.addRegex("([Uu])\\s+\\.\\s+([Ss])\\s+\\.", " $1.$2. ");
  }
  
  public void initialize(ParameterMap params) {
    
  }

  TokenAlignment getStringToWordAlignment(String rawWord, List<String> tokens) {
    List<String> rawChars = SingleCharacterTokenizer.tokenizeStatic(rawWord, null);
    WerAlignmentScorer scorer = new WerAlignmentScorer(rawChars, tokens);
    scorer.setMaxTokensPerChunk(20);
    TokenAlignment alignment = AlignmentGenerator.heuristicAlignment(scorer);
    if (alignment == null) {
      alignment = new TokenAlignment();
      for (int i = 0; i < tokens.size(); i++) {
        alignment.add(0, rawWord.length()-1, i, i);
      }
    }
    return alignment;
  }
  
  protected List<String> getOutputTokens(String rawToken) {
    String processedToken = rawToken;
    processedToken = transformer.process(processedToken);
    processedToken = splitter.process(processedToken);
    processedToken = StringUtils.trim(processedToken);
    String[] tokenArray = processedToken.split("\\s+");
    List<String> tokens = new ArrayList<String>();
    for (String token : tokenArray) {
      if (!isAcronym(token)) {
        token = token.toLowerCase();
      }
      tokens.add(token);
    }
    return tokens;
  }
  
  private boolean isAcronym(String token) {
    int numUpperLetters = 0;
    for (int i = 0; i < token.length(); i++) {
      char c = token.charAt(i);
      String s = ""+c;
      if (s == s.toUpperCase()) {
        numUpperLetters += 1;
      }
    }
    if (token.length() == 0 || token.length() == 1) {
      return false;
    }
    if (token.length() == 2) {
      if (numUpperLetters == 2) {
        return true;
      }
      else {
        return false;
      }
    }
    if ((double)numUpperLetters/(double)token.length() >= 0.65) {
      return true;
    }
    return false;
  }

  @Override
  public List<String> tokenize(String input, TokenAlignment alignment) {
    TokenAlignment rawTokenAlignment = new TokenAlignment();
    List<String> rawTokens = CharacterTokenizer.WHITESPACE.tokenize(input, rawTokenAlignment);
    List<String> outputTokens = new ArrayList<String>();
    List<TokenAlignment> wordAlignment = new ArrayList<TokenAlignment>();
    List<Integer> outputIndexes = new ArrayList<Integer>();
    for (int i = 0; i < rawTokens.size(); i++) {
      String rawToken = rawTokens.get(i);
      List<String> splitTokens = getOutputTokens(rawToken);
      TokenAlignment curAlign = getStringToWordAlignment(rawToken, splitTokens);
      int startIndex = outputTokens.size();
      outputIndexes.add(startIndex);
      for (int j = 0; j < splitTokens.size(); j++) {
        outputTokens.add(splitTokens.get(j));
      }
      wordAlignment.add(curAlign);
    }
    
    if (alignment != null) {
      Map<Range, Range> rawRangeMap = rawTokenAlignment.reverse().createRangeMap();
      for (int i = 0; i < rawTokens.size(); i++) {
        TokenAlignment curAlign = wordAlignment.get(i);
        Range rawRange = rawRangeMap.get(new Range(i, i));
        if (rawRange == null) {
          throw new RuntimeException("Cannot find raw input for token index: "+i);
        }
        int tokenOffset = outputIndexes.get(i);
        int rawStart = rawRange.getStart();
        for (AlignmentPair ap : curAlign) {
          int rawWordStart = ap.getInput().getStart();
          int rawWordEnd = ap.getInput().getEnd();
          int tokenStartIndex = ap.getOutput().getStart();
          int tokenEndIndex = ap.getOutput().getEnd();
          for (int j = tokenStartIndex; j <= tokenEndIndex; j++) {
            alignment.add(rawStart+rawWordStart, rawStart+rawWordEnd, tokenOffset+j, tokenOffset+j);
          }
        }
      }
    }
//    for (AlignmentPair ap : alignment) {
//      int rawStart = ap.getInput().getStart();
//      int rawEnd = ap.getInput().getEnd();
//      int tokStart = ap.getOutput().getStart();
//      int tokEnd = ap.getOutput().getEnd();
//      if (tokStart != tokEnd) {
//        System.exit(1);
//      }
//      System.out.println("TOK "+input.substring(rawStart, rawEnd+1)+" => "+outputTokens.get(tokStart));
//    }
    return outputTokens;
  }
  
  public boolean isLanguageChar(char c) {
    if (characterSet.contains(new Character(c))) {
      return true;
    }
    return false;
  }
}
