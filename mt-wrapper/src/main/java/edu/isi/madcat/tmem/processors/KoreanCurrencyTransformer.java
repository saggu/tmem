package edu.isi.madcat.tmem.processors;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class KoreanCurrencyTransformer extends StringTransformer {
  private Pattern matcherPattern;

  public KoreanCurrencyTransformer() {
    // 10,000 = \uB9CC
    // 1,000 = \uCC9C
    // 100 = \uBC31
    // won = \uC6D0
        
    // @formatter:off
    matcherPattern =
        Pattern.compile("(([0-9,]+)\\uB9CC)?(([0-9,]+)\\uCC9C)?(([0-9,]+)\\uBC31)?([0-9,]+)?\\uC6D0");
    // @formatter:on
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
        if (m.group().length() == 1) { // If the regex ONLY contains "won", skip it
          continue;
        }
        PatternMatcher patternMatcher = new PatternMatcher(m);
        patternMatchers.add(patternMatcher);
      }
    }
    return patternMatchers;
  }

  long getNumber(String str) {
    long number = 0;
    long multiplier = 1;
    for (int i = str.length()-1; i >= 0 && multiplier <= 1000000000000000L; i--) {
      char c = str.charAt(i);
      long digit = -1;
      switch (c) {
        case '0': digit = 0; break;
        case '1': digit = 1; break;
        case '2': digit = 2; break;
        case '3': digit = 3; break;
        case '4': digit = 4; break;
        case '5': digit = 5; break;
        case '6': digit = 6; break;
        case '7': digit = 7; break;
        case '8': digit = 8; break;
        case '9': digit = 9; break;
        default: break;
      }
      if (digit != -1) {
        number += digit*multiplier;
        multiplier *= 10;
      }
    }
    return number;
  }
  
  String addCommas(String str) {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < str.length(); i++) {
      char c = str.charAt(str.length()-1-i);
      sb.append(c);
      if (i > 0 && (i+1) % 3 == 0 && i < str.length()-1) {
        sb.append(",");
      }
    }
    return StringUtils.reverse(sb.toString());
  }
  
  @Override
  public TransformerOutput transformString(String input, PatternMatcher matcher) {
    List<String> groups = matcher.getGroups();
    long number = 0;
    if (groups.get(1) != null) {
      number += getNumber(groups.get(2))*10000;
    }
    if (groups.get(3) != null) {
      number += getNumber(groups.get(4))*1000;
    }
    if (groups.get(5) != null) {
      number += getNumber(groups.get(6))*100;
    }
    if (groups.get(7) != null) {
      number += getNumber(groups.get(7));
    }
    StringBuffer sb = new StringBuffer();
    if (number > 0) {
      String numberString = addCommas(Long.toString(number));
      sb.append(numberString);
      sb.append(" ");
    }
    sb.append("won");
    TransformerOutput output = new TransformerOutput(sb.toString(), "default");
    return output;
  }
}
