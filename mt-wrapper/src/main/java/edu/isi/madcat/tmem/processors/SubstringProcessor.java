package edu.isi.madcat.tmem.processors;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

public class SubstringProcessor {
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  class CoverageSet {
    private List<Pair<Integer, Integer>> ranges;

    public CoverageSet() {
      ranges = new ArrayList<Pair<Integer, Integer>>();
    }

    void add(int start, int end) {
      ranges.add(new ImmutablePair<Integer, Integer>(start, end));
    }

    boolean isCovered(int start, int end) {
      for (int i = 0; i < ranges.size(); i++) {
        if (start <= ranges.get(i).getRight() && end >= ranges.get(i).getLeft()) {
          return true;
        }
      }
      return false;
    }
  }

  public static final String PRESERVE_TOKEN_PREFIX = "bbnpreservezxcvbnm";

  private List<PatternProcessor> processors;

  private String id;

  public SubstringProcessor() {
    id = "sp";
  }

  public List<PatternProcessor> getProcessors() {
    return processors;
  }

  public SubstringOutput processString(String input) {
    SubstringOutput output = new SubstringOutput(this, input);
    CoverageSet cs = new CoverageSet();
    if (!input.contains(SubstringProcessor.PRESERVE_TOKEN_PREFIX)) {
      for (int i = 0; i < processors.size(); i++) {
        PatternProcessor processor = processors.get(i);
        List<PatternMatcher> matchers = new ArrayList<PatternMatcher>();
        if (processor.getTransformer().hasMatcher()) {
          matchers = processor.getTransformer().getMatchers(input);
        } else {
          Pattern pattern = processor.getPattern();
          Matcher m = pattern.matcher(input);
          while (m.find()) {
            matchers.add(new PatternMatcher(m));
          }
        }
        if (matchers != null) {
          for (int j = 0; j < matchers.size(); j++) {
            PatternMatcher m = matchers.get(j);
            int start = m.getStart();
            int end = m.getEnd() - 1;
            if (!cs.isCovered(start, end)) {
              cs.add(start, end);
              output.add(start, end, processor, m);
            }
          }
        }
      }
    }
    output.finalize();
    return output;
  }

  public void setProcessors(List<PatternProcessor> processors) {
    this.processors = processors;
  }
}
