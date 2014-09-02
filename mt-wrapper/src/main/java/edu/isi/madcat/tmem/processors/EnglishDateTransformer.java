package edu.isi.madcat.tmem.processors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class EnglishDateTransformer extends StringTransformer {
  private Pattern numberPattern;
  private Pattern matcherPattern;
  private Pattern fullDatePattern;

  private Map<String, String> monthMap;

  public EnglishDateTransformer() {
    String yearRegex = "(19[0-9]{2}|20[0-9]{2}|(?:`')?[0-9]{2})";
    String monthRegex =
        "(?:((?:01|02|03|04|05|06|07|08|09|1|2|3|4|5|6|7|8|9|10|11|12)|(?:(?:jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)\\S*)))";
    String dayRegex = "([0-9]{1,2})";

    // @formatter:off
    matcherPattern =
        Pattern.compile(dayRegex + "\\s*"+monthRegex+"\\s*"+yearRegex);
    // @formatter:on

    numberPattern = Pattern.compile("^[0-9]+$");
    Pattern.compile("^(\\s*)");

    fullDatePattern = Pattern.compile(dayRegex + "\\s*" + monthRegex + "\\s*" + yearRegex);

    monthMap = new HashMap<String, String>();
    monthMap.put("jan", "1");
    monthMap.put("feb", "2");
    monthMap.put("mar", "3");
    monthMap.put("apr", "4");
    monthMap.put("may", "5");
    monthMap.put("jun", "6");
    monthMap.put("jul", "7");
    monthMap.put("aug", "8");
    monthMap.put("sep", "9");
    monthMap.put("oct", "10");
    monthMap.put("nov", "11");
    monthMap.put("dec", "12");

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
    input = input.toLowerCase();
    List<PatternMatcher> patternMatchers = new ArrayList<PatternMatcher>();
    Matcher m = matcherPattern.matcher(input);
    while (m.find()) {
      if (m.start() != m.end()) {
        PatternMatcher patternMatcher = new PatternMatcher(m);
        patternMatchers.add(patternMatcher);
      }
    }
    return patternMatchers;
  }

  @Override
  public TransformerOutput transformString(String input, PatternMatcher matcher) {
    input = input.toLowerCase();
    Matcher m = fullDatePattern.matcher(input);
    StringBuffer sb = new StringBuffer();
    while (m.find()) {
      String day = m.group(1);
      String month = m.group(2);
      String year = m.group(3);

      if (month != null) {
        if (month.length() > 3) {
          month = month.substring(0, 3);
        }
        if (monthMap.containsKey(month)) {
          month = monthMap.get(month);
        }
      }

      List<String> tokens = new ArrayList<String>();
      if (year != null) {
        tokens.add(year + "\uB144");
      }
      if (month != null) {
        if (numberPattern.matcher(month).find()) {
          tokens.add(month + "\uC6D4");
        } else {
          tokens.add(month);
        }
      }
      if (day != null) {
        tokens.add(day + "\uC77C");
      }
      m.appendReplacement(sb, StringUtils.join(tokens, " "));
    }
    m.appendTail(sb);
    TransformerOutput output = new TransformerOutput(sb.toString(), "default");
    return output;
  }
}
