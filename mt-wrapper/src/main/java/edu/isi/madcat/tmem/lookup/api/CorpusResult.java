package edu.isi.madcat.tmem.lookup.api;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.isi.madcat.tmem.alignment.AlignmentPair;
import edu.isi.madcat.tmem.alignment.Range;
import edu.isi.madcat.tmem.alignment.TokenAlignment;
import edu.isi.madcat.tmem.logging.ExceptionHandler;
import edu.isi.madcat.tmem.lookup.TokToRawAlignment;
import edu.isi.madcat.tmem.sql.SqlUtils;

public class CorpusResult {
  private int segmentId;
  private String source;
  private String target;
  private List<Range> sourceRanges;
  private List<Range> targetRanges;

  public CorpusResult(ResultSet rs, LookupQuery query) {
    try {      
      segmentId = rs.getInt(6);
      source = SqlUtils.getString(rs, 7);
      target = SqlUtils.getString(rs, 9);
      sourceRanges = new ArrayList<Range>();
      targetRanges = new ArrayList<Range>();
      int sourceStart = rs.getInt(2);
      int sourceEnd = rs.getInt(3);
      int targetStart = rs.getInt(4);
      int targetEnd = rs.getInt(5);
      TokToRawAlignment sourceAlignment =
          new TokToRawAlignment(TokenAlignment.fromString(SqlUtils.getString(rs, 11)));
      TokToRawAlignment targetAlignment =
          new TokToRawAlignment(TokenAlignment.fromString(SqlUtils.getString(rs, 12)));
      if (sourceStart != 255) {
        Range sourceRange = sourceAlignment.projectRange(sourceStart, sourceEnd);
        sourceRanges.add(sourceRange);
        if (targetStart == 255) {
          TokenAlignment parallelAlignment = TokenAlignment.fromString(SqlUtils.getString(rs, 13));
          targetRanges = getRanges(parallelAlignment, targetAlignment, sourceStart, sourceEnd);
        }
      }
      if (targetStart != 255) {
        Range targetRange = targetAlignment.projectRange(targetStart, targetEnd);
        targetRanges.add(targetRange);
        if (sourceStart == 255) {
          TokenAlignment parallelAlignment =
              TokenAlignment.fromString(SqlUtils.getString(rs, 13)).reverse();
          sourceRanges = getRanges(parallelAlignment, sourceAlignment, targetStart, targetEnd);
        }
      }
    } catch (SQLException e) {
      ExceptionHandler.handle(e);
    }
  }

  private List<Range> getRanges(TokenAlignment parallelAlignment,
      TokToRawAlignment outputAlignment, int start, int end) {
    List<Integer> outputTokIndexes = new ArrayList<Integer>();
    for (AlignmentPair p : parallelAlignment) {
      int inputIndex = p.getInput().getStart();
      int outputIndex = p.getOutput().getStart();
      if (inputIndex >= start && inputIndex <= end) {
        outputTokIndexes.add(outputIndex);
      }
    }
    List<Range> ranges = new ArrayList<Range>();
    for (int outputTokIndex : outputTokIndexes) {
      Range range = outputAlignment.projectRange(outputTokIndex, outputTokIndex);
      ranges.add(range);
    }
    Collections.sort(ranges);
    if (ranges.size() == 0) {
      return ranges;
    }
    List<Range> compressedRanges = new ArrayList<Range>();
    compressedRanges.add(new Range(ranges.get(0)));
    for (int i = 1; i < ranges.size(); i++) {
      Range iRange = ranges.get(i);
      Range oRange = compressedRanges.get(compressedRanges.size() - 1);
      if ((oRange.getEnd() + 1 >= iRange.getStart()) && (iRange.getEnd() > oRange.getEnd())) {
        oRange.setEnd(iRange.getEnd());
      } else {
        oRange = new Range(iRange);
        compressedRanges.add(oRange);
      }
    }
    return compressedRanges;
  }

  public int getSegmentId() {
    return segmentId;
  }

  public String getSource() {
    return source;
  }

  public List<Range> getSourceRanges() {
    return sourceRanges;
  }

  public String getTarget() {
    return target;
  }

  public List<Range> getTargetRanges() {
    return targetRanges;
  }

  public void setSegmentId(int segmentId) {
    this.segmentId = segmentId;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public void setSourceRanges(List<Range> sourceRanges) {
    this.sourceRanges = sourceRanges;
  }

  public void setTarget(String target) {
    this.target = target;
  }

  public void setTargetRanges(List<Range> targetRanges) {
    this.targetRanges = targetRanges;
  }
}
