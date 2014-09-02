package edu.isi.madcat.tmem.training.tools;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import edu.isi.madcat.tmem.alignment.Range;
import edu.isi.madcat.tmem.logging.ExceptionHandler;
import edu.isi.madcat.tmem.tokenize.CharacterTokenizer;
import edu.isi.madcat.tmem.utils.ParameterMap;
import edu.isi.madcat.tmem.utils.TextSegment;
import edu.isi.madcat.tmem.utils.TextSegmentIterator;
import edu.isi.madcat.tmem.utils.Utils;

public class SentenceSegmentor {
  protected class Segmentor {
    private List<Integer> breakPoints;
    private List<Integer> breakPuncs;
    private List<String> sentence;

    public Segmentor(List<String> sentence) {
      this.sentence = sentence;
    }

    public List<Integer> segmentSentence() {
      breakPuncs = new ArrayList<Integer>();
      breakPoints = new ArrayList<Integer>();
      if (sentence.size() <= mustBreakOver) {
        return breakPoints;
      }
      boolean endsWithPunc = false;
      breakPuncs.add(-1);
      for (int i = 0; i < sentence.size(); i++) {
        if (breakPointPower(sentence.get(i)) > 0) {
          breakPuncs.add(i);
          if (i == sentence.size() - 1) {
            endsWithPunc = true;
          }
        }
      }
      if (!endsWithPunc) {
        breakPuncs.add(sentence.size()); // </s>
      }

      if ((sentence.size() > minBreakOver) || (sentence.size() > mustBreakOver)) {
        splitRecursive(0, breakPuncs.size() - 1);
      }
      return breakPoints;
    }

    private void splitRecursive(int start, int end) {
      if (start + 1 == end) { // no punc in between
        if ((breakPuncs.get(end) - breakPuncs.get(start)) > mustBreakOver) {
          int parts = 1 + (breakPuncs.get(end) - breakPuncs.get(start)) / (mustBreakOver + 1);
          int interval = (breakPuncs.get(end) - breakPuncs.get(start)) / parts;
          if (interval > mustBreakOver) {
            interval = mustBreakOver;
          }
          if (interval == 0) {
            interval = 1;
          }
          for (int i = 1; breakPuncs.get(start) + i * interval < breakPuncs.get(end); i++) {
            breakPoints.add(breakPuncs.get(start) + i * interval);
          }
        }
        return;
      }

      double maxbenefit = 0.0;
      int maxbenidx = -1;
      for (int i = start + 1; i < end; i++) {
        double benefit =
            (breakPuncs.get(i + 1) - breakPuncs.get(i - 1))
                * breakPointPower(sentence.get(breakPuncs.get(i)));
        if (benefit > maxbenefit) {
          maxbenefit = benefit;
          maxbenidx = i;
        }
      }
      if (((breakPuncs.get(maxbenidx) - breakPuncs.get(start)) > minBreakOver)
          || ((breakPuncs.get(maxbenidx) - breakPuncs.get(start)) > mustBreakOver)) {
        splitRecursive(start, maxbenidx);
      }

      breakPoints.add(breakPuncs.get(maxbenidx));

      if (((breakPuncs.get(end) - breakPuncs.get(maxbenidx)) > minBreakOver)
          || ((breakPuncs.get(end) - breakPuncs.get(maxbenidx)) > mustBreakOver)) {
        splitRecursive(maxbenidx, end);
      }
    }
  }

  public static void main(String[] args) {
    ParameterMap params = ParameterMap.readFromArgs(args);
    SentenceSegmentor segmentor = new SentenceSegmentor(params);
    segmentor.process();
  }

  private Map<String, Double> breakPointMapping;
  private String inputField;
  private String[] inputFiles;

  private int minBreakOver;
  private int mustBreakOver;

  private String outputField;

  private String outputFile;

  public SentenceSegmentor(ParameterMap params) {
    inputFiles = params.getStringArrayRequired("input_files");

    String splitTokenFile = params.getStringRequired("split_token_file");
    inputField = params.getStringRequired("input_field");
    minBreakOver = params.getIntRequired("min_break_over");
    mustBreakOver = params.getIntRequired("must_break_over");
    outputField = params.getStringRequired("output_field");
    outputFile = params.getStringRequired("output_file");

    loadSplitTokenFile(splitTokenFile);
  }

  public SentenceSegmentor(String splitTokenFile, int minBreakOver, int mustBreakOver) {
    loadSplitTokenFile(splitTokenFile);
    this.minBreakOver = minBreakOver;
    this.mustBreakOver = mustBreakOver;
  }

  private void loadSplitTokenFile(String splitTokenFile) {
    breakPointMapping = new HashMap<String, Double>();
    Pattern p = Pattern.compile("^(\\S+)\\s*(\\S+)\\s*(\\S+)$");
    List<String> lines = Utils.readLinesFromFile(splitTokenFile);
    for (String line : lines) {
      Matcher m = p.matcher(line);
      if (m.find()) {
        breakPointMapping.put(m.group(1), Double.parseDouble(m.group(3)));
      }
    }
  }

  public List<Range> segmentSentence(List<String> tokens) {
    Segmentor segmentor = new Segmentor(tokens);
    List<Integer> breakPoints = segmentor.segmentSentence();
    List<Range> output = new ArrayList<Range>();
    int startIndex = 0;
    for (int i = 0; i < breakPoints.size() + 1 && startIndex < tokens.size(); i++) {
      int endIndex = tokens.size() - 1;
      if (i < breakPoints.size()) {
        endIndex = breakPoints.get(i);
      }
      if (endIndex >= tokens.size()) {
        endIndex = tokens.size() - 1;
      }
      output.add(new Range(startIndex, endIndex));
      startIndex = endIndex + 1;
    }
    return output;
  }

  public void process() {
    TextSegmentIterator segIt = new TextSegmentIterator(inputFiles);
    try {
      FileWriter writer = new FileWriter(outputFile);
      TextSegment segment = null;
      while ((segment = segIt.next()) != null) {
        List<String> tokens =
            CharacterTokenizer.WHITESPACE.tokenize(segment.getRequired(inputField));
        List<Range> ranges = segmentSentence(tokens);
        List<String> output = new ArrayList<String>();
        for (int i = 0; i < ranges.size(); i++) {
          output.add(i+":"+i+":"+ranges.get(i).toString());
        }
        segment.insert(outputField, StringUtils.join(output, " "));
        segment.write(writer);
      }
      writer.close();
    } catch (IOException e) {
      ExceptionHandler.handle(e);
    }

  }

  private double breakPointPower(String word) {
    Double d = breakPointMapping.get(word);
    if (d == null) {
      return 0.0;
    }
    return d.doubleValue();
  }
}
