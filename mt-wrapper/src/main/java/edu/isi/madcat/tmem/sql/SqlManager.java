package edu.isi.madcat.tmem.sql;

import java.util.ArrayList;
import java.util.List;

import edu.isi.madcat.tmem.utils.ParameterMap;

public class SqlManager {
  private String url;
  private String user;
  private String password;

  public SqlManager(String url, String user, String password) {
    super();
    this.url = url;
    this.user = user;
    this.password = password;
  }

  public SqlManager(String sqlConfigFile) {
    ParameterMap params = new ParameterMap(sqlConfigFile);
    this.url = params.getStringRequired("url");
    this.user = params.getStringRequired("user");
    this.password = params.getString("password");
  }

  public SqlHandler createHandler() {
    SqlHandler handler = new SqlHandler(url, user, password);
    return handler;
  }

  public static <T> List<String> getQuerySetsFromObject(List<T> items) {
    List<String> strings = new ArrayList<String>();
    for (T item : items) {
      strings.add(item.toString());
    }
    return getQuerySets(strings);
  }
  
  public static <T> String getQuerySetFromObject(List<T> items) {
    List<String> strings = new ArrayList<String>();
    for (T item : items) {
      strings.add(item.toString());
    }
    return getQuerySet(strings);
  }

  public static List<String> getQuerySets(List<String> hashValues) {
    return getQuerySets(hashValues, 100);
  }
  
  public static String getQuerySet(List<String> hashValues) {
    return getQuerySets(hashValues, -1).get(0);
  }

  // Since we hash everything, SQL injections can't be used here
  public static List<String> getQuerySets(List<String> hashValues, int maxPerQuery) {
    List<String> output = new ArrayList<String>();
    int numQueries = 1;
    if (maxPerQuery != -1) {
      numQueries = hashValues.size() / maxPerQuery;
      if (hashValues.size() % maxPerQuery != 0) {
        numQueries++;
      }
    }
    int k = 0;
    for (int i = 0; i < numQueries; i++) {
      StringBuilder sb = new StringBuilder();
      for (int j = 0; (maxPerQuery == -1 || j < maxPerQuery) && k < hashValues.size(); j++, k++) {
        if (j > 0) {
          sb.append(",");
        }
        String hashValue = hashValues.get(k);
        sb.append("'" + hashValue + "'");
      }
      output.add(sb.toString());
    }
    return output;
  }
}
