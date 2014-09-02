package edu.isi.madcat.tmem.processors;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlEtcTransformer extends StringTransformer {
  private List<Pattern> patterns;

  public UrlEtcTransformer() {
    patterns = new ArrayList<Pattern>();

    // e-mail
    patterns.add(Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,5}"));

    // URL

    // Case 1. A string starting with one of a few commonly used URL schemes (e.g. http://)
    //         followed by any non-space sequence of characters
    patterns.add(Pattern.compile("(https?|url|file|ssh|ftp)://\\S+"));

    // Case 2. A string starting with www.  followed by any non-space sequence of characters
    patterns.add(Pattern.compile("www\\.\\S+"));

  }

  @Override
  public void setParams(String params) {

  }

  @Override
  public boolean hasMatcher() {
    return true;
  }

  @Override
  public List<PatternMatcher> getMatchers(String input) {
    List<PatternMatcher> patternMatchers = new ArrayList<PatternMatcher>();
    for (Pattern pattern : patterns) {
      Matcher m = pattern.matcher(input);
      if (m.find()) {
        patternMatchers.add(new PatternMatcher(m));
      }
    }
    return patternMatchers;
  }

  @Override
  public TransformerOutput transformString(String input, PatternMatcher matcher) {
    TransformerOutput output = new TransformerOutput(input, "default");
    return output;
  }
}
