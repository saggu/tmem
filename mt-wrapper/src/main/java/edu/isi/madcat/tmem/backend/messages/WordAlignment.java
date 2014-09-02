package edu.isi.madcat.tmem.backend.messages;

import java.util.ArrayList;
import java.util.List;

import edu.isi.madcat.tmem.alignment.AlignmentPair;
import edu.isi.madcat.tmem.alignment.TokenAlignment;
import edu.isi.madcat.tmem.serialization.SimpleSerializable;

public class WordAlignment extends SimpleSerializable {
  protected Long ruleIndex;
  protected Long sourceIndex;
  protected Long targetIndex;

  public WordAlignment() {

  }

  public WordAlignment(Long sourceIndex, Long targetIndex, Long ruleIndex) {
    super();
    this.sourceIndex = sourceIndex;
    this.targetIndex = targetIndex;
    this.ruleIndex = ruleIndex;
  }

  public Long getRuleIndex() {
    return ruleIndex;
  }

  public Long getSourceIndex() {
    return sourceIndex;
  }

  public Long getTargetIndex() {
    return targetIndex;
  }

  public static List<WordAlignment> fromTokenAlignment(TokenAlignment alignment) {
    List<WordAlignment> output = new ArrayList<WordAlignment>();
    for (AlignmentPair p : alignment) {
      for (int i = p.getInput().getStart(); i <= p.getInput().getEnd(); i++) {
        for (int j = p.getOutput().getStart(); j <= p.getOutput().getEnd(); j++) {
          output.add(new WordAlignment(new Long(i), new Long(j), new Long(0)));
        }
      }
    }
    return output;
  }

  public String toString() {
    return sourceIndex+":"+targetIndex+":"+ruleIndex;
  }
}
