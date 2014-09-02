package edu.isi.madcat.tmem.lookup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import edu.isi.madcat.tmem.alignment.Range;
import edu.isi.madcat.tmem.alignment.TokenAlignment;
import edu.isi.madcat.tmem.utils.TextSegment;

public class CorpusTermExtractor {
  private int maxNgramSize;
  private boolean alignedTermsOnly;

  public CorpusTermExtractor(int maxNgramSize, boolean alignedTermsOnly) {
    this.maxNgramSize = maxNgramSize;
    this.alignedTermsOnly = alignedTermsOnly;
  }

  public void addTerms(String rawSource, String[] tokSource, String rawTarget, String[] tokTarget,
      TokenAlignment alignment, TokToRawAlignment sourceAlignment,
      TokToRawAlignment targetAlignment, List<CorpusTerm> corpusTerms) {

    Map<Range, Range> sourceToTargetMap = alignment.createRangeMap();
    Map<Range, Range> targetToSourceMap = alignment.reverse().createRangeMap();
    for (int i = 0; i < tokSource.length; i++) {
      for (int j = 0; j < maxNgramSize && i + j < tokSource.length; j++) {
        int sourceStart = i;
        int sourceEnd = i + j;

        int targetStart = -1;
        int targetEnd = -1;
        for (int k = sourceStart; k <= sourceEnd; k++) {
          Range targetRange = sourceToTargetMap.get(new Range(k, k));
          if (targetRange != null) {
            if (targetStart == -1 || targetRange.getStart() < targetStart) {
              targetStart = targetRange.getStart();
            }
            if (targetEnd == -1 || targetRange.getEnd() > targetEnd) {
              targetEnd = targetRange.getEnd();
            }
          }
        }
        if (targetStart == -1 || targetEnd == -1) {
          continue;
        }

        int bwSourceStart = -1;
        int bwSourceEnd = -1;
        for (int k = targetStart; k <= targetEnd; k++) {
          Range srcRange = targetToSourceMap.get(new Range(k, k));
          if (srcRange != null) {
            if (bwSourceStart == -1 || srcRange.getStart() < bwSourceStart) {
              bwSourceStart = srcRange.getStart();
            }
            if (bwSourceEnd == -1 || srcRange.getEnd() > bwSourceEnd) {
              bwSourceEnd = srcRange.getEnd();
            }
          }
        }

        if (bwSourceStart == -1 || bwSourceEnd == -1) {
          continue;
        }

        if (bwSourceStart < sourceStart || bwSourceEnd > sourceEnd) {
          continue;
        }
        
        Range rawSourceRange = sourceAlignment.projectRange(sourceStart, sourceEnd);
        Range rawTargetRange = targetAlignment.projectRange(targetStart, targetEnd);
        if (rawSourceRange == null || rawTargetRange == null) {
          continue;
        }
        String tokSourceNgram =
            StringUtils.join(Arrays.copyOfRange(tokSource, sourceStart, sourceEnd + 1), " ");
        String tokTargetNgram =
            StringUtils.join(Arrays.copyOfRange(tokTarget, targetStart, targetEnd + 1), " ");
        String rawSourceSpan =
            rawSource.substring(rawSourceRange.getStart(), rawSourceRange.getEnd() + 1);
        String rawTargetSpan =
            rawTarget.substring(rawTargetRange.getStart(), rawTargetRange.getEnd() + 1);

        CorpusTerm corpusTerm =
            new CorpusTerm(rawSourceSpan, tokSourceNgram, rawTargetSpan, tokTargetNgram);
        corpusTerm.setSourceStart(sourceStart);
        corpusTerm.setSourceEnd(sourceEnd);
        corpusTerm.setTargetStart(targetStart);
        corpusTerm.setTargetEnd(targetEnd);
        corpusTerms.add(corpusTerm);
      }
    }
  }

  public void addTerms(String rawSource, String[] tokSource, TokToRawAlignment sourceAlignment,
      List<CorpusTerm> corpusTerms, boolean isBackwards) {
    for (int i = 0; i < tokSource.length; i++) {
      for (int j = 0; j < maxNgramSize && i + j < tokSource.length; j++) {
        int start = i;
        int end = i + j;
        String tokSourceNgram =
            StringUtils.join(Arrays.copyOfRange(tokSource, start, end + 1), " ");
        Range rawRange = sourceAlignment.projectRange(start, end);
        if (rawRange == null) {
          continue;
        }
        String rawSourceSpan = rawSource.substring(rawRange.getStart(), rawRange.getEnd() + 1);
        CorpusTerm corpusTerm = new CorpusTerm(rawSourceSpan, tokSourceNgram, isBackwards);
        if (isBackwards) {
          corpusTerm.setTargetStart(start);
          corpusTerm.setTargetEnd(end);
        } else {
          corpusTerm.setSourceStart(start);
          corpusTerm.setSourceEnd(end);
        }
        corpusTerms.add(corpusTerm);
      }
    }
  }

  public List<CorpusTerm> extractTerms(TextSegment segment) {
    String rawSource = segment.getRequired("RAW_SOURCE");
    String[] tokSource = StringUtils.split(segment.getRequired("TOKENIZED_SOURCE"), " ");
    String rawTarget = segment.getRequired("RAW_TARGET");
    String[] tokTarget = StringUtils.split(segment.getRequired("TOKENIZED_TARGET"), " ");
    TokenAlignment alignment = TokenAlignment.fromString(segment.getRequired("ALIGNMENT"));
    TokToRawAlignment sourceAlignment =
        new TokToRawAlignment(TokenAlignment.fromString(segment.getRequired("SOURCE_ALIGNMENT")));
    TokToRawAlignment targetAlignment =
        new TokToRawAlignment(TokenAlignment.fromString(segment.getRequired("TARGET_ALIGNMENT")));

    List<CorpusTerm> corpusTerms = new ArrayList<CorpusTerm>();
    addTerms(rawSource, tokSource, rawTarget, tokTarget, alignment, sourceAlignment,
        targetAlignment, corpusTerms);

    if (!alignedTermsOnly) {
      addTerms(rawSource, tokSource, sourceAlignment, corpusTerms, false);
      addTerms(rawTarget, tokTarget, targetAlignment, corpusTerms, true);
    }
    return corpusTerms;
  }

}
