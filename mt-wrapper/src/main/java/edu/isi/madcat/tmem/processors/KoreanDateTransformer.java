package edu.isi.madcat.tmem.processors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class KoreanDateTransformer extends StringTransformer {
  private Pattern yearPattern;
  private Pattern matcherPattern;
  private Pattern leadingWhitespacePattern;
  private Pattern fullDatePattern;

  private Map<String, String> capMonths;
  private Map<String, String> lowMonths;

  public KoreanDateTransformer() {
    String yearRegex = "(([`'\\u2019][0-9]{2}|[0-9]{4})\\uB144)";
    String monthRegex = "(([0-9]+)\uC6D4)";
    String dayRegex = "(([0-9]+)\uC77C)";

    // @formatter:off
    matcherPattern =
        Pattern.compile("(" + yearRegex + "\\s*"+monthRegex+"?\\s*"+dayRegex+"?)"
            + "|(" + yearRegex + "?\\s*"+monthRegex+"\\s*"+dayRegex+"?)"
            + "|(" + yearRegex + "?\\s*"+monthRegex+"?\\s*"+dayRegex+")");
    // @formatter:on

    leadingWhitespacePattern = Pattern.compile("^(\\s*)");
    yearPattern = Pattern.compile("[`'\\u2019]([0-9]{2})|([0-9]{4})");

    fullDatePattern = Pattern.compile(yearRegex + "?\\s*" + monthRegex + "?\\s*" + dayRegex + "?");
    capMonths = new HashMap<String, String>();
    capMonths.put("1", "JAN");
    capMonths.put("2", "FEB");
    capMonths.put("3", "MAR");
    capMonths.put("4", "APR");
    capMonths.put("5", "MAY");
    capMonths.put("6", "JUN");
    capMonths.put("7", "JUL");
    capMonths.put("8", "AUG");
    capMonths.put("9", "SEP");
    capMonths.put("01", "JAN");
    capMonths.put("02", "FEB");
    capMonths.put("03", "MAR");
    capMonths.put("04", "APR");
    capMonths.put("05", "MAY");
    capMonths.put("06", "JUN");
    capMonths.put("07", "JUL");
    capMonths.put("08", "AUG");
    capMonths.put("09", "SEP");
    capMonths.put("10", "OCT");
    capMonths.put("11", "NOV");
    capMonths.put("12", "DEC");

    lowMonths = new HashMap<String, String>();
    lowMonths.put("1", "Jan");
    lowMonths.put("2", "Feb");
    lowMonths.put("3", "Mar");
    lowMonths.put("4", "Apr");
    lowMonths.put("5", "May");
    lowMonths.put("6", "Jun");
    lowMonths.put("7", "Jul");
    lowMonths.put("8", "Aug");
    lowMonths.put("9", "Sep");
    lowMonths.put("01", "Jan");
    lowMonths.put("02", "Feb");
    lowMonths.put("03", "Mar");
    lowMonths.put("04", "Apr");
    lowMonths.put("05", "May");
    lowMonths.put("06", "Jun");
    lowMonths.put("07", "Jul");
    lowMonths.put("08", "Aug");
    lowMonths.put("09", "Sep");
    lowMonths.put("10", "Oct");
    lowMonths.put("11", "Nov");
    lowMonths.put("12", "Dec");
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
    Matcher m = fullDatePattern.matcher(input);
    StringBuffer sb = new StringBuffer();
    while (m.find()) {
      String year = m.group(2);
      String month = m.group(4);
      String day = m.group(6);
      if (year != null) {
        Matcher ym = yearPattern.matcher(year);
        if (ym.find()) {
          if (ym.group(1) != null && ym.group(1).length() == 2) {
            year = "20" + ym.group(1);
          } else if (ym.group(2) != null && ym.group(2).length() == 4) {
            year = ym.group(2);
          }
        }
      }

      if (month != null) {
        Map<String, String> lookupMonths = lowMonths;
        StringBuffer dateSb = new StringBuffer();
        if (lookupMonths.containsKey(month)) {
          month = lookupMonths.get(month);
        }
      }

      List<String> tokens = new ArrayList<String>();
      if (day != null) {
        tokens.add(day);
      }
      if (month != null) {
        tokens.add(month);
      }
      if (year != null) {
        tokens.add(year);
      }
      Matcher wsm = leadingWhitespacePattern.matcher(input);
      StringBuilder dateSb = new StringBuilder();
      if (wsm.find()) {
        dateSb.append(wsm.group(1));
      }
      if (tokens.size() > 0) {
        dateSb.append(StringUtils.join(tokens, " "));
      }
      m.appendReplacement(sb, dateSb.toString());
    }
    m.appendTail(sb);
    TransformerOutput output = new TransformerOutput(sb.toString(), "default");
    return output;
  }
}
