package edu.isi.madcat.tmem.tokenize.transformers.korean;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.isi.madcat.tmem.tokenize.transformers.StaticTransformer;

public class KoreanDateTransformer extends StaticTransformer {
  private static Pattern yearPattern;
  private static Pattern fullDatePattern;

  private static Map<String, String> capMonths;
  private static Map<String, String> lowMonths;

  static {
    yearPattern =
        Pattern.compile("[`'\\u2019]([0-9]{2})|([0-9]{4})");

    fullDatePattern =
        Pattern.compile("([`'\\u2019][0-9]{2}|[0-9]{4})\\uB144\\s*([0-9]+)\uC6D4\\s*([0-9]+)\uC77C");
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
    capMonths.put("1", "Jan.");
    capMonths.put("2", "Feb.");
    capMonths.put("3", "Mar.");
    capMonths.put("4", "Apr.");
    capMonths.put("5", "May");
    capMonths.put("6", "Jun.");
    capMonths.put("7", "Jul.");
    capMonths.put("8", "Aug.");
    capMonths.put("9", "Sep.");
    capMonths.put("01", "Jan.");
    capMonths.put("02", "Feb.");
    capMonths.put("03", "Mar.");
    capMonths.put("04", "Apr.");
    capMonths.put("05", "May");
    capMonths.put("06", "Jun.");
    capMonths.put("07", "Jul.");
    capMonths.put("08", "Aug.");
    capMonths.put("09", "Sep.");
    capMonths.put("10", "Oct.");
    capMonths.put("11", "Nov.");
    capMonths.put("12", "Dec.");
  }

  @Override
  public String transform(String input) {
    System.out.println("DEBUG: "+input);
    Matcher m = fullDatePattern.matcher(input);
    StringBuffer sb = new StringBuffer();
    while (m.find()) {
      String year = m.group(1);
      String month = m.group(2);
      String day = m.group(3);
      StringBuffer dateSb = new StringBuffer();
      if (capMonths.containsKey(month)) {
        month = capMonths.get(month);
      }
      Matcher ym = yearPattern.matcher(year);
      if (ym.find()) {
        if (ym.group(1).length() == 2) {
          year = "20"+ym.group(1);
        }
        else if (ym.group(2).length() == 4) {
          year = ym.group(2);
        }
      }
      dateSb.append(day+" "+month+" "+year);
      m.appendReplacement(sb, dateSb.toString());
    }
    m.appendTail(sb);
    
    String output = sb.toString();
    return output;
  }
}
