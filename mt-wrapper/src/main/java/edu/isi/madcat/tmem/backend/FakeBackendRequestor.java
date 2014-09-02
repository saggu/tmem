package edu.isi.madcat.tmem.backend;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;

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

public class FakeBackendRequestor extends BackendRequestor {
  protected String transform;

  @Override
  public SegmentorResponse segment(SegmentorRequest request) throws CateProcessException {
    List<Long> segmentationPoints = new ArrayList<Long>();
    SegmentorResponse response = new SegmentorResponse(segmentationPoints);
    return response;
  }

  public void setTransform(String transform) {
    this.transform = transform;
  }

  @Override
  public TokenizerResponse tokenize(TokenizerRequest request) throws CateProcessException {
    Random rng = new Random(request.getText().hashCode());
    List<String> inputTokens = CharacterTokenizer.WHITESPACE.tokenize(request.getText());
    List<String> outputTokens = new ArrayList<String>();
    for (int i = 0; i < inputTokens.size(); i++) {
      if (rng.nextDouble() <= 0.2) {
        String inputToken = inputTokens.get(i);
        int startChartIndex = 0;
        int numSubTokens = 0;
        for (int j = 0; j < inputToken.length(); j++) {
          if (rng.nextDouble() <= 0.4 && numSubTokens < 2) {
            String token = inputToken.substring(startChartIndex, j + 1);
            outputTokens.add(token);
            startChartIndex = j + 1;
            numSubTokens++;
          }
        }
        if (startChartIndex < inputToken.length()) {
          String token = inputToken.substring(startChartIndex, inputToken.length());
          outputTokens.add(token);
        }
      } else if (rng.nextDouble() <= 0.3) {
        int numWords = rng.nextInt(Math.min(3, inputTokens.size() - i));
        String token = StringUtils.join(inputTokens.subList(i, i + numWords), "");
        outputTokens.add(token);
        i += numWords - 1;
      } else {
        outputTokens.add(inputTokens.get(i));
      }
    }
    TokenizerResponse response = new TokenizerResponse(outputTokens);
    return response;
  }

  @Override
  public TranslationResponse translate(TranslationRequest request) throws CateProcessException {
    List<TranslationHypothesis> hypotheses = new ArrayList<TranslationHypothesis>();
    for (int i = 0; i < request.getNbestSize(); i++) {
      List<String> translatedWords = new ArrayList<String>();
      List<String> detokenizedWords = new ArrayList<String>();
      List<WordAlignment> wordAlignment = new ArrayList<WordAlignment>();
      Double totalScore = new Double(-(double) (i + 1) * 10.0);
      for (int j = 0; j < request.getSourceWords().size(); j++) {
        String word = request.getSourceWords().get(j);
        translatedWords.add(transformWord(word));
        detokenizedWords.add(transformWord(word));
        wordAlignment.add(new WordAlignment(new Long(j), new Long(j), new Long(j)));
      }

      TranslationHypothesis hypothesis =
          new TranslationHypothesis(Integer.toString(i), translatedWords, detokenizedWords,
              wordAlignment, totalScore);
      hypotheses.add(hypothesis);
    }
    TranslationResponse response = new TranslationResponse();
    response.setHypotheses(hypotheses);
    return response;
  }

  protected String transformWord(String word) {
    return word;
  }
}
