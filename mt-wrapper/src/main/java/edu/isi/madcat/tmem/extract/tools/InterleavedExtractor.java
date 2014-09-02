package edu.isi.madcat.tmem.extract.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import edu.isi.madcat.tmem.alignment.Range;
import edu.isi.madcat.tmem.logging.ExceptionHandler;
import edu.isi.madcat.tmem.tokenize.Tokenizer;
import edu.isi.madcat.tmem.tokenize.TokenizerFactory;
import edu.isi.madcat.tmem.utils.ParameterMap;
import edu.isi.madcat.tmem.utils.TextSegment;
import edu.isi.madcat.tmem.utils.Utils;

public class InterleavedExtractor {
  private enum LineLabel {
    SOURCE, TARGET, BLANK
  }

  class LabelSpan {
    private int start;

    private int end;

    private LineLabel label;

    public LabelSpan(int start, int end, LineLabel label) {
      super();
      this.start = start;
      this.end = end;
      this.label = label;
    }

    public int getEnd() {
      return end;
    }

    public LineLabel getLabel() {
      return label;
    }

    public int getStart() {
      return start;
    }

    public void setEnd(int end) {
      this.end = end;
    }

    public void setLabel(LineLabel label) {
      this.label = label;
    }

    public void setStart(int start) {
      this.start = start;
    }
  }

  class LexCounts {
    private Map<String, Double> jointCounts;
    private Map<String, Double> sourceCounts;
    private Map<String, Double> targetCounts;

    public LexCounts(String inputFile) {
      jointCounts = new HashMap<String, Double>();
      sourceCounts = new HashMap<String, Double>();
      targetCounts = new HashMap<String, Double>();

      Pattern p = Pattern.compile("^(.*) (.*) (.*)$");
      BufferedReader reader = null;
      try {
        reader = new BufferedReader(new InputStreamReader(Utils.openFile(inputFile)));
        String line = null;
        while ((line = reader.readLine()) != null) {
          Matcher m = p.matcher(line);
          if (!m.find()) {
            reader.close();
            throw new RuntimeException("Malformed line: " + line);
          }
          String source = m.group(1);
          String target = m.group(2);
          double count = Double.parseDouble(m.group(3));
          String joint = source + " " + target;
          addToMap(joint, count, jointCounts);
          addToMap(source, count, sourceCounts);
          addToMap(target, count, targetCounts);
        }
      } catch (FileNotFoundException e) {
        ExceptionHandler.handle(e);
      } catch (IOException e) {
        ExceptionHandler.handle(e);
      } finally {
        IOUtils.closeQuietly(reader);
      }
    }

    private void addToMap(String key, double count, Map<String, Double> counts) {
      Double d = counts.get(key);
      if (d == null) {
        counts.put(key, count);
      } else {
        counts.put(key, d.doubleValue() + count);
      }
    }

    private double getJointCount(String source, String target) {
      Double d = jointCounts.get(source + " " + target);
      if (d == null) {
        return 0.0;
      }
      return d;
    }

    private double getSourceCount(String source) {
      Double d = sourceCounts.get(source);
      if (d == null) {
        return 0.0;
      }
      return d;
    }

    private double getTargetCount(String target) {
      Double d = targetCounts.get(target);
      if (d == null) {
        return 0.0;
      }
      return d;
    }

    double getBwProb(String source, String target) {
      double jointCount = getJointCount(source, target);
      double targetCount = getTargetCount(target);
      double prob = 0.0;
      if (targetCount > 0.0) {
        prob = jointCount / targetCount;
      }
      return prob;
    }

    double getFwProb(String source, String target) {
      double jointCount = getJointCount(source, target);
      double sourceCount = getSourceCount(source);
      double prob = 0.0;
      if (sourceCount > 0.0) {
        prob = jointCount / sourceCount;
      }
      return prob;
    }
  }

  class SpanRange {
    private int sourceStart;

    private int sourceEnd;

    private int targetStart;

    private int targetEnd;

    public SpanRange(int sourceStart, int sourceEnd, int targetStart, int targetEnd) {
      super();
      this.sourceStart = sourceStart;
      this.sourceEnd = sourceEnd;
      this.targetStart = targetStart;
      this.targetEnd = targetEnd;
    }

    public int getSourceEnd() {
      return sourceEnd;
    }

    public int getSourceStart() {
      return sourceStart;
    }

    public int getTargetEnd() {
      return targetEnd;
    }

    public int getTargetStart() {
      return targetStart;
    }

    public void setSourceEnd(int sourceEnd) {
      this.sourceEnd = sourceEnd;
    }

    public void setSourceStart(int sourceStart) {
      this.sourceStart = sourceStart;
    }

    public void setTargetEnd(int targetEnd) {
      this.targetEnd = targetEnd;
    }

    public void setTargetStart(int targetStart) {
      this.targetStart = targetStart;
    }

    @Override
    public String toString() {
      return "SpanRange [sourceStart=" + sourceStart + ", sourceEnd=" + sourceEnd
          + ", targetStart=" + targetStart + ", targetEnd=" + targetEnd + "]";
    }
  }

  public static void main(String[] args) {
    if (args.length != 1) {
      throw new RuntimeException("Usage: [app] parameter_file");
    }

    String parameterFile = args[0];
    ParameterMap params = new ParameterMap(parameterFile);
    InterleavedExtractor processor = new InterleavedExtractor(params);
    processor.process();
  }

  private Tokenizer sourceTokenizer;

  private Tokenizer targetTokenizer;

  private String inputFileList;

  private LexCounts lexCounts;

  private double minSourcePercent;

  private double maxTokenRatio;

  private double tokenSmoothing;

  private double goodMatchProb;
  private double goodMatchCount;

  private double medMatchProb;
  private double medMatchCount;

  private double minMatchToKeep;

  private Set<String> functionWords;

  private String outputFile;

  public InterleavedExtractor(ParameterMap params) {
    inputFileList = params.getStringRequired("input_file_list");
    String sourceTokenizerName = params.getStringRequired("source_tokenizer_name");
    String sourceTokenizerParamFile = params.getString("source_tokenizer_param_file");
    String targetTokenizerName = params.getStringRequired("target_tokenizer_name");
    String targetTokenizerParamFile = params.getString("target_tokenizer_param_file");
    String functionWordFile = params.getString("function_word_file");
    String lexCountsFile = params.getString("lex_counts_file");
    outputFile = params.getString("output_file");
    minSourcePercent = 0.3;
    maxTokenRatio = 2.0;
    tokenSmoothing = 10.0;
    minMatchToKeep = 0.3;
    sourceTokenizer = TokenizerFactory.create(sourceTokenizerName, sourceTokenizerParamFile);
    targetTokenizer = TokenizerFactory.create(targetTokenizerName, targetTokenizerParamFile);
    lexCounts = null;
    if (lexCountsFile != null) {
      lexCounts = new LexCounts(lexCountsFile);
    }
    functionWords = null;
    if (functionWordFile != null) {
      functionWords = new HashSet<String>();
      List<String> lines = null;
      try {
        lines = FileUtils.readLines(new File(functionWordFile));
      } catch (IOException e) {
        ExceptionHandler.handle(e);
      }
      for (String line : lines) {
        functionWords.add(line);
      }
    }
    goodMatchProb = 0.05;
    goodMatchCount = 3.0;
    medMatchProb = 0.005;
    medMatchCount = 1.0;
  }

  public void process() {
    List<String> inputFiles = null;
    try {
      inputFiles = FileUtils.readLines(new File(inputFileList));
    } catch (IOException e) {
      ExceptionHandler.handle(e);
    }
    List<TextSegment> allSegments = new ArrayList<TextSegment>();
    for (int i = 0; i < inputFiles.size(); i++) {
      List<String> lines = null;
      try {
        lines = FileUtils.readLines(new File(inputFiles.get(i)));
      } catch (IOException e) {
        ExceptionHandler.handle(e);
      }
      List<TextSegment> segments = extractSegments(i, lines);
      allSegments.addAll(segments);
    }
    Writer writer = Utils.createWriter(outputFile);
    for (TextSegment segment : allSegments) {
      segment.write(writer);
    }
    IOUtils.closeQuietly(writer);
  }

  private String cleanupString(String input) {
    String str = input;
    str = str.replaceAll("\\.\\s*\\.\\s*\\.(((\\.)|(\\s))+)", " ... ");
    str = str.replaceAll("\\s+", " ");
    str = str.replaceAll("(\n|\r|\u0085|\u2028|\u2029)", " ");
    str = str.replaceAll("^\\s+", "");
    str = str.replaceAll("\\s+$", "");
    return str;
  }

  private List<TextSegment> extractSegments(int fileIndex, List<String> lines) {
    int segmentIndex = 0;
    List<TextSegment> segments = new ArrayList<TextSegment>();
    List<String> cleanLines = new ArrayList<String>();
    for (String line : lines) {
      cleanLines.add(cleanupString(line));
    }
    List<LineLabel> lineLabels = new ArrayList<LineLabel>();
    for (int i = 0; i < cleanLines.size(); i++) {
      String line = cleanLines.get(i);
      LineLabel label = LineLabel.BLANK;
      if (line.length() > 0) {
        int sourceCharCount = 0;
        int targetCharCount = 0;
        for (int j = 0; j < line.length(); j++) {
          char c = line.charAt(j);
          if (sourceTokenizer.isLanguageChar(c)) {
            sourceCharCount++;
          } else if (targetTokenizer.isLanguageChar(c)) {
            targetCharCount++;
          }
        }
        int totalCount = sourceCharCount + targetCharCount;
        if (totalCount > 0) {
          double sourcePercent = (double) sourceCharCount / (double) totalCount;
          if (sourcePercent >= minSourcePercent) {
            label = LineLabel.SOURCE;
          } else {
            label = LineLabel.TARGET;
          }
        }
      }
      lineLabels.add(label);
    }
    List<LabelSpan> labelSpans = new ArrayList<LabelSpan>();
    for (int i = 0; i < lineLabels.size(); i++) {
      if (i == 0) {
        labelSpans.add(new LabelSpan(0, 0, lineLabels.get(i)));
      } else {
        if (lineLabels.get(i) == lineLabels.get(i - 1)) {
          labelSpans.get(labelSpans.size() - 1).setEnd(i);
        } else {
          labelSpans.add(new LabelSpan(i, i, lineLabels.get(i)));
        }
      }
    }
    List<List<String>> tokenizedLines = new ArrayList<List<String>>();
    for (int i = 0; i < cleanLines.size(); i++) {
      String line = cleanLines.get(i);
      List<String> tokens = null;
      LineLabel label = lineLabels.get(i);
      if (label == LineLabel.SOURCE) {
        tokens = sourceTokenizer.tokenize(line);
      } else if (label == LineLabel.TARGET) {
        tokens = targetTokenizer.tokenize(line);
      } else {
        tokens = new ArrayList<String>();
      }
      tokenizedLines.add(tokens);
    }
    for (int i = 0; i < labelSpans.size(); i++) {
      LabelSpan labelSpan = labelSpans.get(i);
      LineLabel label = labelSpan.getLabel();
      // int start = labelSpan.getStart();
      // int end = labelSpan.getEnd();
      // System.out.println("Label Span [" + i + "]: " + start + " " + end + " " + label);
      // for (int j = start; j <= end; j++) {
      // System.out.println(tokenizedLines.get(j));
      // }
      if (label == LineLabel.TARGET) {
        List<SpanRange> spanCands = getSpanCandidates(labelSpans, i);
        if (spanCands != null && spanCands.size() > 0) {
          List<Double> simScores = new ArrayList<Double>();
          for (int j = 0; j < spanCands.size(); j++) {
            double score = getSimScore(spanCands.get(j), labelSpans, tokenizedLines);
            simScores.add(score);
          }
          double bestScore = 0.0;
          int bestIndex = -1;
          for (int j = 0; j < spanCands.size(); j++) {
            double score = simScores.get(j);
            if (j == 0 || score > bestScore) {
              bestScore = score;
              bestIndex = j;
            }
          }
          if (bestScore > 0.0) {
            SpanRange bestSpanRange = spanCands.get(bestIndex);
            Range sourceLineRange =
                getLineRange(labelSpans, bestSpanRange.getSourceStart(), bestSpanRange
                    .getSourceEnd());
            String rawSource =
                getJoinedLines(cleanLines, sourceLineRange.getStart(), sourceLineRange.getEnd());
            Range targetLineRange =
                getLineRange(labelSpans, bestSpanRange.getTargetStart(), bestSpanRange
                    .getTargetEnd());
            String rawTarget =
                getJoinedLines(cleanLines, targetLineRange.getStart(), targetLineRange.getEnd());
            TextSegment segment = new TextSegment();
            String guid =
                String.format("[ExtractedSegments][File_%05d][Segment_%05d]", fileIndex,
                    segmentIndex);
            segment.insert("GUID", guid);
            segment.insert("RAW_SOURCE", rawSource);
            segment.insert("RAW_TARGET", rawTarget);
            segment.insert("TOKENIZED_SOURCE", StringUtils.join(
                sourceTokenizer.tokenize(rawSource), " "));
            segment.insert("TOKENIZED_TARGET", StringUtils.join(
                targetTokenizer.tokenize(rawTarget), " "));
            segments.add(segment);
            segmentIndex++;
          }
        }
      }
    }
    return segments;
  }

  private String getJoinedLines(List<String> lines, int start, int end) {
    StringBuilder sb = new StringBuilder();
    for (int i = start; i <= end; i++) {
      if (i > start) {
        sb.append(" ");
      }
      sb.append(lines.get(i));
    }
    return cleanupString(sb.toString());
  }

  private Range getLineRange(List<LabelSpan> labelSpans, int spanStart, int spanEnd) {
    int start = labelSpans.get(spanStart).getStart();
    int end = labelSpans.get(spanEnd).getEnd();
    return new Range(start, end);
  }

  private double getSimScore(SpanRange spanRange, List<LabelSpan> labelSpans,
      List<List<String>> tokenizedLines) {
    List<String> sourceWords = new ArrayList<String>();
    List<String> targetWords = new ArrayList<String>();
    for (int i = spanRange.getSourceStart(); i <= spanRange.getSourceEnd(); i++) {
      int start = labelSpans.get(i).getStart();
      int end = labelSpans.get(i).getEnd();
      for (int j = start; j <= end; j++) {
        sourceWords.addAll(tokenizedLines.get(j));
      }
    }
    for (int i = spanRange.getTargetStart(); i <= spanRange.getTargetEnd(); i++) {
      int start = labelSpans.get(i).getStart();
      int end = labelSpans.get(i).getEnd();
      for (int j = start; j <= end; j++) {
        targetWords.addAll(tokenizedLines.get(j));
      }
    }
    double fwRatio =
        ((double) sourceWords.size() + tokenSmoothing)
            / ((double) targetWords.size() + tokenSmoothing);
    double bwRatio =
        ((double) targetWords.size() + tokenSmoothing)
            / ((double) sourceWords.size() + tokenSmoothing);
    double ratio = Math.max(fwRatio, bwRatio);
    if (ratio > maxTokenRatio) {
      return 0.0;
    }
    double goodCount = 0.0;
    double medCount = 0.0;
    double totalCount = 0.0;
    for (int i = 0; i < sourceWords.size(); i++) {
      String source = sourceWords.get(i);
      if (functionWords != null && functionWords.contains(source)) {
        continue;
      }
      boolean isGood = false;
      boolean isMed = false;
      for (int j = 0; j < targetWords.size(); j++) {
        String target = targetWords.get(j);
        if (functionWords != null && functionWords.contains(target)) {
          continue;
        }
        if (source.equals(target)) {
          isGood = true;
        }
        double jointCount = lexCounts.getJointCount(source, target);
        double fwProb = lexCounts.getFwProb(source, target);
        double bwProb = lexCounts.getBwProb(source, target);
        if (jointCount >= goodMatchCount && fwProb >= goodMatchProb && bwProb >= goodMatchProb) {
          isGood = true;
        } else if (jointCount >= medMatchCount && fwProb >= medMatchProb && bwProb >= medMatchProb) {
          isMed = true;
        }
      }
      if (isGood) {
        goodCount += 1.0;
      } else if (isMed) {
        medCount += 1.0;
      }
      totalCount += 1.0;
    }
    if (totalCount == 0.0) {
      return 0.0;
    }
    System.out.println("***");
    System.out.println(StringUtils.join(sourceWords, " "));
    System.out.println(StringUtils.join(targetWords, " "));
    System.out.println("totalCount: " + totalCount);
    System.out.println("goodCount: " + goodCount);
    System.out.println("medCount: " + medCount);
    System.out.println("");
    double medProb = goodCount + medCount / totalCount;
    if (medProb >= minMatchToKeep) {
      return 1.0;
    }
    return 0.0;
  }

  private List<SpanRange> getSpanCandidates(List<LabelSpan> labelSpans, int index) {
    List<SpanRange> spanCands = new ArrayList<SpanRange>();
    boolean isValid = false;
    int sourceStart = -1;
    for (int i = index + 1; i < labelSpans.size(); i++) {
      LineLabel label = labelSpans.get(i).getLabel();
      if (label == LineLabel.SOURCE) {
        isValid = true;
        sourceStart = i;
        break;
      } else if (label == LineLabel.TARGET) {
        break;
      }
    }
    if (!isValid) {
      return null;
    }
    int targetStart = index;
    int targetEnd = index;
    int sourceEnd = sourceStart;
    for (int i = targetStart - 1; i >= 0; i--) {
      LineLabel label = labelSpans.get(i).getLabel();
      if (label == LineLabel.TARGET) {
        targetStart = i;
        break;
      } else if (label == LineLabel.SOURCE) {
        break;
      }
    }
    for (int i = sourceStart; i < labelSpans.size(); i++) {
      LineLabel label = labelSpans.get(i).getLabel();
      if (label == LineLabel.SOURCE) {
        sourceEnd = i;
        break;
      } else if (label == LineLabel.TARGET) {
        break;
      }
    }
    for (int i = targetStart; i <= targetEnd; i++) {
      for (int j = sourceStart; j <= sourceEnd; j++) {
        SpanRange spanRange = new SpanRange(sourceStart, j, i, targetEnd);
        spanCands.add(spanRange);
      }
    }
    return spanCands;
  }
}
