package edu.isi.madcat.tmem.backend;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import edu.isi.madcat.tmem.alignment.TokenAlignment;
import edu.isi.madcat.tmem.backend.messages.SegmentorRequest;
import edu.isi.madcat.tmem.backend.messages.SegmentorResponse;
import edu.isi.madcat.tmem.backend.messages.TokenizerRequest;
import edu.isi.madcat.tmem.backend.messages.TokenizerResponse;
import edu.isi.madcat.tmem.backend.messages.TranslationHypothesis;
import edu.isi.madcat.tmem.backend.messages.TranslationRequest;
import edu.isi.madcat.tmem.backend.messages.TranslationResponse;
import edu.isi.madcat.tmem.backend.messages.WordAlignment;
import edu.isi.madcat.tmem.exceptions.CateProcessException;
import edu.isi.madcat.tmem.tokenize.CharacterTokenizer;

public class TestBackendRequestor extends BackendRequestor {
  private static class ParsedTokenPatterns {
    static Pattern sourcePattern;
    static Pattern numSplitsPattern;
    static Pattern endOfSegmentPattern;
    static Pattern targetPattern;

    static {
      sourcePattern = Pattern.compile("^([sS]\\d+($\\d+_\\d+)?)");
      numSplitsPattern = Pattern.compile(":(\\d+)");
      endOfSegmentPattern = Pattern.compile("#");
      targetPattern = Pattern.compile("T([0-9_]+)");
    }
  }

  private class ParsedToken {
    private String sourceString;

    private int numSplits;

    private boolean isEndOfSegment;

    private List<Integer> targetIndexes;

    ParsedToken(String str) {
      Matcher m = null;

      sourceString = "S";
      m = ParsedTokenPatterns.sourcePattern.matcher(str);
      if (m.find()) {
        sourceString = m.group(1);
      }

      numSplits = 1;
      m = ParsedTokenPatterns.numSplitsPattern.matcher(str);
      if (m.find()) {
        numSplits = Integer.parseInt(m.group(1));
      }

      isEndOfSegment = false;
      m = ParsedTokenPatterns.endOfSegmentPattern.matcher(str);
      if (m.find()) {
        isEndOfSegment = true;
      }

      targetIndexes = new ArrayList<Integer>();
      m = ParsedTokenPatterns.targetPattern.matcher(str);
      if (m.find()) {
        String[] tokens = m.group(1).split("_");
        for (int i = 0; i < tokens.length; i++) {
          targetIndexes.add(new Integer(tokens[i]));
        }
      }
    }

    List<String> getSourceTokens() {
      List<String> source = new ArrayList<String>();
      for (int i = 0; i < numSplits; i++) {
        StringBuilder sb = new StringBuilder();
        sb.append(sourceString);
        sb.append("$" + i + "_" + numSplits);
        if (isEndOfSegment) {
          sb.append("#");
        }
        sb.append("T");
        sb.append(StringUtils.join(targetIndexes, "_"));
        source.add(sb.toString());
      }
      return source;
    }

  }
  private int getNumTargetWords(List<ParsedToken> parsedTokens) {
    int numTargetWords = 0;
    for (ParsedToken token : parsedTokens) {
      for (Integer index : token.targetIndexes) {
        if (index.intValue() + 1 > numTargetWords) {
          numTargetWords = index.intValue() + 1;
        }
      }
    }
    return numTargetWords;
  }

  @Override
  public SegmentorResponse segment(SegmentorRequest request) throws CateProcessException {
    List<Long> segmentationPoints = new ArrayList<Long>();
    for (int i = 0; i < request.getSourceWords().size(); i++) {
      ParsedToken token = new ParsedToken(request.getSourceWords().get(i));
      if (token.isEndOfSegment) {
        segmentationPoints.add(new Long(i));
      }
    }
    return new SegmentorResponse(segmentationPoints);
  }

  @Override
  public TokenizerResponse tokenize(TokenizerRequest request) throws CateProcessException {
    TokenAlignment rawToInputAlignment = new TokenAlignment();
    List<String> inputTokens = CharacterTokenizer.WHITESPACE.tokenize(request.getText(), rawToInputAlignment);
    List<String> outputTokens = new ArrayList<String>();
    TokenAlignment inputToOutputAlignment = new TokenAlignment();
    int outputOffset = 0;
    for (int i = 0; i < inputTokens.size(); i++) {
      ParsedToken token = new ParsedToken(inputTokens.get(i));
      List<String> tokens = token.getSourceTokens();
      inputToOutputAlignment.add(i, i, outputOffset, outputOffset+tokens.size()-1);
      outputOffset += tokens.size();
      outputTokens.addAll(tokens);
    }
    TokenAlignment alignment = TokenAlignment.projectAlignment(rawToInputAlignment, inputToOutputAlignment);
    TokenizerResponse response = new TokenizerResponse(outputTokens, WordAlignment.fromTokenAlignment(alignment));
    return response;
  }

  @Override
  public TranslationResponse translate(TranslationRequest request) throws CateProcessException {
    List<ParsedToken> parsedTokens = new ArrayList<ParsedToken>();
    for (int i = 0; i < request.getSourceWords().size(); i++) {
      ParsedToken token = new ParsedToken(request.getSourceWords().get(i));
      parsedTokens.add(token);
    }
    SortedMap<Integer, SortedSet<Integer>> alignmentMap = new TreeMap<Integer, SortedSet<Integer>>();
    for (int i = 0; i < parsedTokens.size(); i++) {
      Integer sourceIndex = new Integer(i);
      for (Integer targetIndex : parsedTokens.get(i).targetIndexes) {
        SortedSet<Integer> targetIndexSet = alignmentMap.get(sourceIndex);
        if (targetIndexSet == null) {
          targetIndexSet = new TreeSet<Integer>();
          alignmentMap.put(sourceIndex, targetIndexSet);
        }
        targetIndexSet.add(targetIndex);
      }
    }
    List<WordAlignment> wordAlignment = new ArrayList<WordAlignment>();
    for (Map.Entry<Integer, SortedSet<Integer>> entry : alignmentMap.entrySet()) {
      Integer sourceIndex = entry.getKey();
      SortedSet<Integer> targetIndexSet = entry.getValue();
      for (Integer targetIndex : targetIndexSet) {
        wordAlignment.add(new WordAlignment(sourceIndex.longValue(), targetIndex.longValue(), sourceIndex.longValue()));
      }
    }
    int numTargetWords = getNumTargetWords(parsedTokens);
    List<String> translatedWords = new ArrayList<String>();
    for (int i = 0; i < numTargetWords; i++) {
      String targetToken = "T"+i;
      translatedWords.add(targetToken);
    }
    List<String> detokenizedWords = new ArrayList<String>(translatedWords);

    TranslationHypothesis hyp =
        new TranslationHypothesis("0", translatedWords, detokenizedWords, wordAlignment, -100.0);
    List<TranslationHypothesis> hyps = new ArrayList<TranslationHypothesis>();
    hyps.add(hyp);
    TranslationResponse response = new TranslationResponse();
    response.setHypotheses(hyps);
    return response;
  }

}
