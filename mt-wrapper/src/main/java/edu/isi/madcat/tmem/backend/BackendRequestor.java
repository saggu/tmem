package edu.isi.madcat.tmem.backend;

import edu.isi.madcat.tmem.backend.messages.SegmentorRequest;
import edu.isi.madcat.tmem.backend.messages.SegmentorResponse;
import edu.isi.madcat.tmem.backend.messages.TokenizerRequest;
import edu.isi.madcat.tmem.backend.messages.TokenizerResponse;
import edu.isi.madcat.tmem.backend.messages.TranslationRequest;
import edu.isi.madcat.tmem.backend.messages.TranslationResponse;
import edu.isi.madcat.tmem.exceptions.CateProcessException;

public abstract class BackendRequestor {

  public abstract SegmentorResponse segment(SegmentorRequest request) throws CateProcessException;

  public abstract TokenizerResponse tokenize(TokenizerRequest request) throws CateProcessException;

  public abstract TranslationResponse translate(TranslationRequest request)
      throws CateProcessException;
}
