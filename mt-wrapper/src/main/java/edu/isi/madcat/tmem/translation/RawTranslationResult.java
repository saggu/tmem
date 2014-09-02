package edu.isi.madcat.tmem.translation;

import java.util.List;

import edu.isi.madcat.tmem.backend.messages.SegmentorRequest;
import edu.isi.madcat.tmem.backend.messages.SegmentorResponse;
import edu.isi.madcat.tmem.backend.messages.TokenizerRequest;
import edu.isi.madcat.tmem.backend.messages.TokenizerResponse;
import edu.isi.madcat.tmem.backend.messages.TranslationRequest;
import edu.isi.madcat.tmem.backend.messages.TranslationResponse;

public class RawTranslationResult {
  public TokenizerRequest getTokenizerRequest() {
    return tokenizerRequest;
  }

  public void setTokenizerRequest(TokenizerRequest tokenizerRequest) {
    this.tokenizerRequest = tokenizerRequest;
  }

  public TokenizerResponse getTokenizerResponse() {
    return tokenizerResponse;
  }

  public void setTokenizerResponse(TokenizerResponse tokenizerResponse) {
    this.tokenizerResponse = tokenizerResponse;
  }

  public SegmentorRequest getSegmentorRequest() {
    return segmentorRequest;
  }

  public void setSegmentorRequest(SegmentorRequest segmentorRequest) {
    this.segmentorRequest = segmentorRequest;
  }

  public SegmentorResponse getSegmentorResponse() {
    return segmentorResponse;
  }

  public void setSegmentorResponse(SegmentorResponse segmentorResponse) {
    this.segmentorResponse = segmentorResponse;
  }

  public List<TranslationRequest> getTranslationRequests() {
    return translationRequests;
  }

  public void setTranslationRequests(List<TranslationRequest> translationRequests) {
    this.translationRequests = translationRequests;
  }

  public List<TranslationResponse> getTranslationResponses() {
    return translationResponses;
  }

  public void setTranslationResponses(List<TranslationResponse> translationResponses) {
    this.translationResponses = translationResponses;
  }

  private TokenizerRequest tokenizerRequest;
  private TokenizerResponse tokenizerResponse;
  private SegmentorRequest segmentorRequest;
  private SegmentorResponse segmentorResponse;
  private List<TranslationRequest> translationRequests;
  private List<TranslationResponse> translationResponses;
}
