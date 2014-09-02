package edu.isi.madcat.tmem.translation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.isi.madcat.tmem.alignment.AlignmentPair;
import edu.isi.madcat.tmem.alignment.Range;
import edu.isi.madcat.tmem.alignment.TokenAlignment;

public class TranslatedSegment {
  public static TranslatedSegment createEmptySegment(int id) {
    TranslatedSegment seg = new TranslatedSegment();
    seg.id = id;
    seg.inputString = "";
    seg.sourceWords = new ArrayList<String>();
    seg.tokenizedTargetWords = new ArrayList<String>();
    seg.targetWords = new ArrayList<OutputToken>();
    seg.srcRawToSrcTokAlign = new TokenAlignment();
    seg.srcTokToTrgTokAlign = new TokenAlignment();
    seg.srcTokToTrgDetokAlign = new TokenAlignment();
    seg.srcRawToTrgDetokAlign = new TokenAlignment();
    seg.trgTokToTrgDetokAlign = new TokenAlignment();
    seg.empty = true;
    seg.phrases = null;
    return seg;
  }

  public static TranslatedSegment emptySegment(int id, String inputString) {
    TranslatedSegment segment = new TranslatedSegment();
    segment.id = id;
    segment.inputString = inputString;
    segment.empty = true;
    return segment;
  }

  protected boolean empty;

  protected int id;

  protected String inputString;

  protected TokenAlignment srcRawToSrcTokAlign;

  protected TokenAlignment srcTokToTrgTokAlign;

  protected TokenAlignment srcTokToTrgDetokAlign;

  protected TokenAlignment srcRawToTrgDetokAlign;

  protected TokenAlignment trgTokToTrgDetokAlign;

  protected List<String> sourceWords;

  protected List<String> tokenizedTargetWords;

  protected List<OutputToken> targetWords;

  protected List<DisplayPhrase> phrases;

  protected Map<Integer, Integer> targetWordToPhrase;

  public TranslatedSegment() {
    super();
  }

  public TranslatedSegment(int id, String inputString, List<String> sourceWords,
      List<String> tokenizedTargetWords, List<OutputToken> targetWords,
      TokenAlignment srcRawToSrcTokAlign, TokenAlignment srcTokToTrgTokAlign,
      TokenAlignment srcTokToTrgDetokAlign, TokenAlignment srcRawToTrgDetokAlign,
      TokenAlignment trgTokToTrgDetokAlign) {
    super();
    this.id = id;
    this.inputString = inputString;
    this.sourceWords = sourceWords;
    this.tokenizedTargetWords = tokenizedTargetWords;
    this.targetWords = targetWords;
    this.srcRawToSrcTokAlign = srcRawToSrcTokAlign;
    this.srcTokToTrgTokAlign = srcTokToTrgTokAlign;
    this.srcTokToTrgDetokAlign = srcTokToTrgDetokAlign;
    this.srcRawToTrgDetokAlign = srcRawToTrgDetokAlign;
    this.trgTokToTrgDetokAlign = trgTokToTrgDetokAlign;
    this.empty = false;
    this.phrases = null;
  }

  public void createDisplayPhrases() {
    if (phrases != null) {
      return;
    }
    Map<Integer, List<Integer>> targetToTokSource = createTargetToTokSourceMap();
    Map<Integer, List<Range>> tokSourceToRawSource = new HashMap<Integer, List<Range>>();
    for (AlignmentPair p : getSrcRawToSrcTokAlign()) {
      for (int index = p.getOutput().getStart(); index <= p.getOutput().getEnd(); index++) {
        List<Range> ranges = tokSourceToRawSource.get(index);
        if (ranges == null) {
          ranges = new ArrayList<Range>();
          tokSourceToRawSource.put(index, ranges);
        }
        ranges.add(p.getInput());
      }
    }
    phrases = new ArrayList<DisplayPhrase>();
    targetWordToPhrase = new HashMap<Integer, Integer>();
    for (int i = 0; i < getTargetWords().size(); i++) {
      List<Integer> srcIndexes = targetToTokSource.get(i);
      List<Range> spans = new ArrayList<Range>();
      OutputToken targetString = getTargetWords().get(i);
      if (srcIndexes != null) {
        for (int srcIndex : srcIndexes) {
          List<Range> ranges = tokSourceToRawSource.get(srcIndex);
          if (ranges != null) {
            for (Range range : ranges) {
              spans.add(range);
            }
          }
        }
      }      
      DisplayPhrase phrase = new DisplayPhrase(spans, targetString.getWord());
      phrase.setTokenJoiner(targetString.getJoiner());
      targetWordToPhrase.put(i, i);
      phrases.add(phrase);
    }
  }

  public Map<Integer, List<Integer>> createTargetToTokSourceMap() {
    Map<Integer, List<Integer>> targetToTokSource = new HashMap<Integer, List<Integer>>();
    for (AlignmentPair p : getSrcTokToTrgDetokAlign()) {
      for (int trgIndex = p.getOutput().getStart(); trgIndex <= p.getOutput().getEnd(); trgIndex++) {
        List<Integer> srcIndexes = targetToTokSource.get(trgIndex);
        if (srcIndexes == null) {
          srcIndexes = new ArrayList<Integer>();
          targetToTokSource.put(trgIndex, srcIndexes);
        }
        for (int srcIndex = p.getInput().getStart(); srcIndex <= p.getInput().getEnd(); srcIndex++) {
          srcIndexes.add(srcIndex);
        }
      }
    }
    return targetToTokSource;
  }

  public int getId() {
    return id;
  }

  public String getInputString() {
    return inputString;
  }

  public String getJoinedTargetWords() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < targetWords.size(); i++) {
      OutputToken token = targetWords.get(i);
      sb.append(token.getWord());
      boolean doAppendSpace = false;
      if (i < targetWords.size() - 1) {
        OutputToken nextToken = targetWords.get(i+1);
        if (token.getJoiner() != TokenJoiner.RIGHT && nextToken.getJoiner() != TokenJoiner.LEFT) {
          doAppendSpace = true;
        }
      }
      if (doAppendSpace) {
        sb.append(" ");
      }
    }
    return sb.toString();
  }

  public List<DisplayPhrase> getPhrases() {
    return phrases;
  }

  public List<String> getSourceWords() {
    return sourceWords;
  }

  public TokenAlignment getSrcRawToSrcTokAlign() {
    return srcRawToSrcTokAlign;
  }

  public TokenAlignment getSrcRawToTrgDetokAlign() {
    return srcRawToTrgDetokAlign;
  }

  public TokenAlignment getSrcTokToTrgDetokAlign() {
    return srcTokToTrgDetokAlign;
  }

  public TokenAlignment getSrcTokToTrgTokAlign() {
    return srcTokToTrgTokAlign;
  }

  public List<OutputToken> getTargetWords() {
    return targetWords;
  }

  public Map<Integer, Integer> getTargetWordToPhrase() {
    return targetWordToPhrase;
  }

  public List<String> getTokenizedTargetWords() {
    return tokenizedTargetWords;
  }

  public TokenAlignment getTrgTokToTrgDetokAlign() {
    return trgTokToTrgDetokAlign;
  }

  public boolean isEmpty() {
    return empty;
  }

  public void setPhrases(List<DisplayPhrase> phrases) {
    this.phrases = phrases;
  }

  public void setSrcTokToTrgTokAlign(TokenAlignment srcTokToTrgTokAlign) {
    this.srcTokToTrgTokAlign = srcTokToTrgTokAlign;
  }

  public void setTargetWords(List<OutputToken> targetWords) {
    this.targetWords = targetWords;
  }

  public void setTargetWordToPhrase(Map<Integer, Integer> targetWordToPhrase) {
    this.targetWordToPhrase = targetWordToPhrase;
  }

  public void setTokenizedTargetWords(List<String> tokenizedTargetWords) {
    this.tokenizedTargetWords = tokenizedTargetWords;
  }

  public void setTrgTokToTrgDetokAlign(TokenAlignment trgTokToTrgDetokAlign) {
    this.trgTokToTrgDetokAlign = trgTokToTrgDetokAlign;
  }

  @Override
  public String toString() {
    return "TranslatedSegment [empty=" + empty + ", id=" + id + ", inputString=" + inputString
        + ", srcRawToSrcTokAlign=" + srcRawToSrcTokAlign + ", srcTokToTrgTokAlign="
        + srcTokToTrgTokAlign + ", srcTokToTrgDetokAlign=" + srcTokToTrgDetokAlign
        + ", srcRawToTrgDetokAlign=" + srcRawToTrgDetokAlign + ", trgTokToTrgDetokAlign="
        + trgTokToTrgDetokAlign + ", sourceWords=" + sourceWords + ", tokenizedTargetWords="
        + tokenizedTargetWords + ", targetWords=" + targetWords + ", phrases=" + phrases
        + ", targetWordToPhrase=" + targetWordToPhrase + "]";
  }
}
