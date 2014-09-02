package edu.isi.madcat.tmem.processors;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

public class PatternMatcher {

  public int getStart() {
    return start;
  }

  public void setStart(int start) {
    this.start = start;
  }

  public int getEnd() {
    return end;
  }

  public void setEnd(int end) {
    this.end = end;
  }

  public List<String> getGroups() {
    return groups;
  }

  public void setGroups(List<String> groups) {
    this.groups = groups;
  }

  private int start;
  private int end;
  private List<String> groups;

  public PatternMatcher() {

  }

  public PatternMatcher(Matcher m) {
    super();
    groups = new ArrayList<String>();
    start = m.start();
    end = m.end();
    for (int i = 0; i <= m.groupCount(); i++) {
      groups.add(m.group(i));
    }
  }

}
