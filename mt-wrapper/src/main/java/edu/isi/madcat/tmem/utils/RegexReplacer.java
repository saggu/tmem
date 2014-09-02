package edu.isi.madcat.tmem.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexReplacer {
  class FullPattern {
    public String getRegex() {
      return regex;
    }

    public void setRegex(String regex) {
      this.regex = regex;
    }

    public String getReplace() {
      return replace;
    }

    public void setReplace(String replace) {
      this.replace = replace;
    }

    public int getPatternFlags() {
      return patternFlags;
    }

    public void setPatternFlags(int patternFlags) {
      this.patternFlags = patternFlags;
    }

    public Pattern getPattern() {
      return pattern;
    }

    public void setPattern(Pattern pattern) {
      this.pattern = pattern;
    }

    private String regex;
    private String replace;
    private int patternFlags;
    private Pattern pattern;
    
    public FullPattern(String regex, String replace, int patternFlags) {
      super();
      this.regex = regex;
      this.replace = replace;
      this.patternFlags = patternFlags;
      this.pattern = Pattern.compile(regex);
    }
  }
  
  protected List<FullPattern> patterns;
  
  public RegexReplacer() {
    patterns = new ArrayList<FullPattern>();
  }
  
  public void addRegex(String regex, String replace, int patternFlags) {
    FullPattern fullPattern = new FullPattern(regex, replace, patternFlags);
    patterns.add(fullPattern);
  }
  
  public void addRegex(String regex, String replace) {
    addRegex(regex, replace, 0);
  }
  
  public String process(String input) {
    for (FullPattern p : patterns) {
      Matcher m = p.getPattern().matcher(input);
      input = m.replaceAll(p.getReplace());
    }
    return input;
  }
}
