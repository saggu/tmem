package edu.isi.madcat.tmem.alignment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import edu.isi.madcat.tmem.backend.messages.WordAlignment;
import edu.isi.madcat.tmem.tokenize.CharacterTokenizer;


public class TokenAlignment implements Iterable<AlignmentPair> {
  static Pattern fullAlignPattern;
  static Pattern simpleAlignPattern;
  
  static {
    fullAlignPattern = Pattern.compile("^(\\d+):(\\d+):(\\d+):(\\d+)$");
    simpleAlignPattern = Pattern.compile("^(\\d+):(\\d+)$");
  }
  
  public static TokenAlignment fromString(String inputString) {
    TokenAlignment output = new TokenAlignment();
    String[] tokens = StringUtils.split(inputString, " ");
    for (String token: tokens) {
      Matcher matcher = fullAlignPattern.matcher(token);
      if (matcher.find()) {
        output.add(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)), Integer.parseInt(matcher.group(4)));
      }
      else {
        matcher = simpleAlignPattern.matcher(token);
        if (matcher.find()) {
          output.add(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(2)));
        }
      }
    }
    return output;
  }

  public static TokenAlignment fromWordAlignment(List<WordAlignment> alignment) {
    TokenAlignment output = new TokenAlignment();
    for (WordAlignment a : alignment) {
      output.add(a.getSourceIndex().intValue(), a.getSourceIndex().intValue(), a.getTargetIndex().intValue(), a.getTargetIndex().intValue());
    }
    output.sortAndCompress();
    return output;
  }

  public static TokenAlignment projectAlignment(TokenAlignment a, TokenAlignment b) {
    TokenAlignment out = new TokenAlignment();
    for (int i = 0; i < a.p.size(); i++) {
      List<Range> ranges = new ArrayList<Range>();
      for (int j = 0; j < b.p.size(); j++) {
        int overlap = a.p.get(i).getOutput().computeOverlap(b.p.get(j).getInput());
        if (overlap > 0) {
          ranges.add(b.p.get(j).getOutput());
        }
      }
      for (Range range : ranges) {
        out.add(a.p.get(i).getInput(), range);
      }
    }
    out.sortAndCompress();
    return out;
  }

  public static TokenAlignment unityAlignment(int length) {
    TokenAlignment alignment = new TokenAlignment();
    for (int i = 0; i < length; i++) {
      alignment.add(i, i, i, i);
    }
    return alignment;
  }

  protected List<AlignmentPair> p;

  public TokenAlignment() {
    p = new ArrayList<AlignmentPair>();
  }

  public TokenAlignment(TokenAlignment other) {
    p = new ArrayList<AlignmentPair>();
    for (AlignmentPair ap : other) {
      p.add(new AlignmentPair(ap));
    }
  }

  public TokenAlignment(List<WordAlignment> input) {
    p = new ArrayList<AlignmentPair>();
    for (WordAlignment wordAlignment : input) {
      if (wordAlignment.getSourceIndex().intValue() >= 0
          && wordAlignment.getTargetIndex().intValue() >= 0) {
        add(wordAlignment.getSourceIndex().intValue(), wordAlignment.getSourceIndex().intValue(),
            wordAlignment.getTargetIndex().intValue(), wordAlignment.getTargetIndex().intValue());
      }
    }
  }

  public TokenAlignment(Map<Range, Range> mapping) {
    p = new ArrayList<AlignmentPair>();
    for (Map.Entry<Range, Range> r : mapping.entrySet()) {
      add(r.getKey(), r.getValue());
    }
  }

  public TokenAlignment(String str) {
    p = new ArrayList<AlignmentPair>();
    List<String> tokens = CharacterTokenizer.WHITESPACE.tokenize(str);
    for (String t : tokens) {
      p.add(new AlignmentPair(t));
    }
  }

  public void add(AlignmentPair a) {
    p.add(a);
  }

  public void add(int as, int ae, int bs, int be) {
    p.add(new AlignmentPair(new Range(as, ae), new Range(bs, be)));
  }

  public void add(Range a, Range b) {
    p.add(new AlignmentPair(a, b));
  }

  public Map<Range, Range> createRangeMap() {
    Map<Range, Range> mapping = new TreeMap<Range, Range>();
    for (AlignmentPair alignment : p) {
      mapping.put(alignment.getInput(), alignment.getOutput());
    }
    return mapping;
  }

  public void extendAlignment(int inputOffset, int outputOffset, TokenAlignment newAlignment) {
    for (AlignmentPair alignment : newAlignment) {
      add(inputOffset + alignment.getInput().getStart(), inputOffset
          + alignment.getInput().getEnd(), outputOffset + alignment.getOutput().getStart(),
          outputOffset + alignment.getOutput().getEnd());
    }
  }

  public String getAlignmentString(List<String> a, List<String> b) {
    StringBuilder sb = new StringBuilder();
    for (AlignmentPair pair : this) {
      List<String> subA = a.subList(pair.getInput().getStart(), pair.getInput().getEnd() + 1);
      List<String> subB = b.subList(pair.getOutput().getStart(), pair.getOutput().getEnd() + 1);
      sb.append(subA.toString());
      sb.append(" => ");
      sb.append(subB.toString());
      sb.append("\n");
    }
    return sb.toString();
  }

  public Iterator<AlignmentPair> iterator() {
    return p.iterator();
  }

  public TokenAlignment reverse() {
    TokenAlignment output = new TokenAlignment();
    for (AlignmentPair a : this) {
      output.add(a.getOutput(), a.getInput());
    }
    return output;
  }

  public int size() {
    return p.size();
  }

  public void sort() {
    Collections.sort(p);
  }

  public void sortAndCompress() {
    Collections.sort(p);
    List<AlignmentPair> output = new ArrayList<AlignmentPair>();
    for (int i = 0; i < p.size(); i++) {
      AlignmentPair currPair = p.get(i);
      if (i > 0 && currPair.getInput().equals(output.get(output.size() - 1).getInput())) {
        Range range = currPair.getOutput();
        if (range.getStart() <= output.get(output.size() - 1).getOutput().getEnd() + 1) {
          Range prevRange = output.get(output.size() - 1).getOutput();
          int newStart = Math.min(prevRange.getStart(), range.getStart());
          int newEnd = Math.max(prevRange.getEnd(), range.getEnd());
          output.set(output.size() - 1, new AlignmentPair(currPair.getInput(), new Range(newStart,
              newEnd)));
        } else {
          output.add(currPair);
        }
      } else {
        output.add(currPair);
      }
    }
    p = output;
    output = new ArrayList<AlignmentPair>();
    for (int i = 0; i < p.size(); i++) {
      AlignmentPair currPair = p.get(i);
      if (i > 0 && currPair.getOutput().equals(output.get(output.size() - 1).getOutput())) {
        Range range = currPair.getInput();
        if (range.getStart() <= output.get(output.size() - 1).getInput().getEnd() + 1) {
          Range prevRange = output.get(output.size() - 1).getInput();
          int newStart = Math.min(prevRange.getStart(), range.getStart());
          int newEnd = Math.max(prevRange.getEnd(), range.getEnd());
          output.set(output.size() - 1, new AlignmentPair(new Range(newStart, newEnd), currPair
              .getOutput()));
        } else {
          output.add(currPair);
        }
      } else {
        output.add(currPair);
      }
    }
    p = output;
  }

  public String toPairString() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < p.size(); i++) {
      if (i > 0) {
        sb.append(" ");
      }
      sb.append(p.get(i).getInput().getStart());
      sb.append(":");
      sb.append(p.get(i).getOutput().getStart());
    }
    return sb.toString();
  }
  
  public String toSimpleString() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < p.size(); i++) {
      if (i > 0) {
        sb.append(" ");
      }
      sb.append(p.get(i).toString());
    }
    return sb.toString();
  }


  @Override
  public String toString() {
    return p.toString();
  }
}
