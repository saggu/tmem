package edu.isi.madcat.tmem.processors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;

public class HierarchicalTransformer implements InitializingBean {

  public class IndexType {
    public int getIndex() {
      return index;
    }

    public void setIndex(int index) {
      this.index = index;
    }

    public String getProcessType() {
      return processType;
    }

    public void setProcessType(String processType) {
      this.processType = processType;
    }

    public String getJoinType() {
      return joinType;
    }

    public void setJoinType(String joinType) {
      this.joinType = joinType;
    }

    private int index;
    private String processType;
    private String joinType;

    public IndexType() {
      super();
    }

    public void fromString(String str) {
      Pattern indexPattern = Pattern.compile("^(\\d+):([A-Z])([A-Z]{0,1})$");
      Matcher m = indexPattern.matcher(str);
      if (!m.find()) {
        throw new RuntimeException("Malformed pattern: " + str);
      }
      index = Integer.parseInt(m.group(1));
      processType = m.group(2);
      joinType = m.group(3);
    }
  }

  public class GroupMatcher {
    public int getIndex() {
      return index;
    }

    public void setIndex(int index) {
      this.index = index;
    }

    public String getMatcherFunc() {
      return matcherFunc;
    }

    public void setMatcherFunc(String matcherFunc) {
      this.matcherFunc = matcherFunc;
    }

    public Map<String, String> getMatcherParams() {
      return matcherParams;
    }

    public void setMatcherParams(Map<String, String> matcherParams) {
      this.matcherParams = matcherParams;
    }

    private int index;
    private String matcherFunc;
    private Map<String, String> matcherParams;

    public GroupMatcher(int index, String matcherFunc, Map<String, String> matcherParams) {
      super();
      this.index = index;
      this.matcherFunc = matcherFunc;
      this.matcherParams = matcherParams;
    }

  }

  private static Pattern matcherPattern;
  private static Pattern equalMatcher;

  static {
    matcherPattern = Pattern.compile("^(\\d+):(.*?):(.*)$");
    equalMatcher = Pattern.compile("^(.*)=(.*)$");
  }

  private Pattern pattern;

  private String indexString;

  private String matcherString;

  private List<IndexType> indexes;

  private List<GroupMatcher> matchers;

  public HierarchicalTransformer() {
    super();
    indexes = new ArrayList<IndexType>();
    matchers = new ArrayList<GroupMatcher>();
  }

  public List<IndexType> getIndexes() {
    return indexes;
  }

  public String getIndexString() {
    return indexString;
  }

  public String getMatcherString() {
    return matcherString;
  }

  public Pattern getPattern() {
    return pattern;
  }

  public void setIndexes(List<IndexType> indexes) {
    this.indexes = indexes;
  }

  public void setIndexString(String indexString) {
    this.indexString = indexString;
    indexes = new ArrayList<IndexType>();
    String[] array = StringUtils.split(indexString, " ");
    for (String a : array) {
      IndexType indexType = new IndexType();
      indexType.fromString(a);
      indexes.add(indexType);
    }
  }

  public void setMatcherString(String matcherString) {
    this.matcherString = matcherString;
    String[] values = StringUtils.split(matcherString, " ");

    for (String value : values) {
      Matcher m = matcherPattern.matcher(value);
      if (!m.find()) {
        throw new RuntimeException("Malformed matcher: " + value);
      }
      int groupIndex = Integer.parseInt(m.group(1));
      String matcherFunc = m.group(2);
      Map<String, String> matcherParams = parseMatcherParams(m.group(3));
      GroupMatcher sm = new GroupMatcher(groupIndex, matcherFunc, matcherParams);
      matchers.add(sm);
    }
  }

  public Map<String, String> parseMatcherParams(String matcherParamsString) {
    Map<String, String> map = new HashMap<String, String>();
    String[] strs = StringUtils.split(matcherParamsString, ",");
    for (String str : strs) {
      Matcher m = equalMatcher.matcher(str);
      if (!m.find()) {
        throw new RuntimeException("Malformed matcher: " + str);
      }
      String key = m.group(1);
      String value = m.group(2);
      map.put(key, value);
    }
    return map;
  }

  public void setPattern(Pattern pattern) {
    this.pattern = pattern;
  }

  public List<String> getMatchGroups(String input) {
    Matcher m = pattern.matcher(input);
    if (!m.find()) {
      return null;
    }
    List<String> groups = new ArrayList<String>();
    for (int i = 0; i <= m.groupCount(); i++) {
      groups.add(m.group(i));
    }
    for (int i = 0; i < matchers.size(); i++) {
      GroupMatcher matcher = matchers.get(i);
      String group = groups.get(matcher.getIndex());
      boolean dm = doesMatch(matcher, group);
      if (!dm) {
        return null;
      }
    }
    return groups;
  }

  private boolean doesMatch(GroupMatcher matcher, String group) {
    Map<String, String> params = matcher.getMatcherParams();
    if (matcher.getMatcherFunc().equals("num_words")) {
      int n = Integer.parseInt(params.get("n"));
      int m = Integer.parseInt(params.get("m"));
      String[] values = StringUtils.trim(group).split("\\s+");
      if (values.length >= n && values.length <= m) {
        return true;
      }
      return false;
    }
    throw new RuntimeException("Unknown matcher type: " + matcher);
  }
  
  public void afterPropertiesSet() throws Exception {
    Matcher m = pattern.matcher("");
    int numGroups = m.groupCount();
    for (IndexType indexType : indexes) {
      if (indexType.getIndex() > numGroups) {
        throw new RuntimeException("Group index "+indexType.getIndex()+" out of range for pattern: "+pattern.toString());
      }
    }
  }
}
