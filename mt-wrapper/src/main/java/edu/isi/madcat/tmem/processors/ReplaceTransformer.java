package edu.isi.madcat.tmem.processors;

import java.util.ArrayList;
import java.util.List;

public class ReplaceTransformer extends StringTransformer {
  private List<String> delims;
  private List<Integer> groups;

  @Override
  public TransformerOutput transformString(String input, PatternMatcher matcher) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < groups.size(); i++) {
      sb.append(delims.get(i));
      sb.append(matcher.getGroups().get(groups.get(i)));
    }
    sb.append(delims.get(delims.size() - 1));
    TransformerOutput output = new TransformerOutput(sb.toString(), "default");
    return output;
  }

  @Override
  public void setParams(String params) {
    delims = new ArrayList<String>();
    groups = new ArrayList<Integer>();

    char[] chars = params.toCharArray();
    char state = 'D';
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < chars.length; i++) {
      char c = chars[i];
      if (state == 'D') {
        if (c == '\\') { // start of escape
          state = 'E';
        } else if (c == '$') { // start of group
          delims.add(sb.toString());
          sb = new StringBuilder();
          state = 'G';
        } else {
          sb.append(c);
        }
      } else if (state == 'E') { // the next character after the escape
        state = 'D';
        sb.append(c);
      } else if (state == 'G') {
        if (c == '$') { // end of group
          int groupId = Integer.parseInt(sb.toString());
          sb = new StringBuilder();
          groups.add(groupId);
          state = 'D';
        } else {
          sb.append(c);
        }
      }
    }
    delims.add(sb.toString());
  }

}
