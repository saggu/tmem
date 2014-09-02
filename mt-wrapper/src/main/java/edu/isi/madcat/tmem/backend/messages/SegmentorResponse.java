package edu.isi.madcat.tmem.backend.messages;

import java.util.ArrayList;
import java.util.List;

import edu.isi.madcat.tmem.serialization.SimpleSerializable;

public class SegmentorResponse extends SimpleSerializable {
  protected List<Long> segmentationPoints;

  public SegmentorResponse() {

  }

  public SegmentorResponse(List<Long> segmentationPoints) {
    super();
    this.segmentationPoints = segmentationPoints;
  }

  public List<List<String>> generateSplitSegments(SegmentorRequest request) {
    ArrayList<List<String>> output = new ArrayList<List<String>>();
    int start = 0;
    for (Long point : segmentationPoints) {
      int end = point.intValue();
      List<String> subSegment = request.getSourceWords().subList(start, end);
      start = end;
      output.add(subSegment);
    }
    output.add(request.getSourceWords().subList(start, request.getSourceWords().size()));
    return output;
  }

  public List<Long> getSegmentationPoints() {
    return segmentationPoints;
  }
}
