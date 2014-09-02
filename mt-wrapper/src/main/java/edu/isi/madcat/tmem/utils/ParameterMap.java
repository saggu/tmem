package edu.isi.madcat.tmem.utils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.isi.madcat.tmem.logging.ExceptionHandler;

public class ParameterMap {
  protected static Pattern splitter;
  protected static Map<String, String> staticVariables;

  static {
    splitter = Pattern.compile("\\s+");
    staticVariables = new HashMap<String, String>();
  }

  public static void setStaticVariables(Map<String, String> staticVariables) {
    ParameterMap.staticVariables = staticVariables;
  }

  public static void addStaticVariables(Map<String, String> staticVariables) {
    for (Map.Entry<String, String> e : staticVariables.entrySet()) {
      addStaticVariable(e.getKey(), e.getValue());
    }
  }

  public static void addStaticVariables(ParameterMap staticVariables) {
    addStaticVariables(staticVariables.params);
  }

  public static void setStaticVariables(ParameterMap staticVariables) {
    setStaticVariables(staticVariables.params);
  }

  public static void addStaticVariable(String key, String value) {
    staticVariables.put(key,  value);
  }

  public static ParameterMap readFromArgs(String[] args) {
    if (args.length != 1) {
      throw new RuntimeException("Parameter file not specified.");
    }
    ParameterMap params = new ParameterMap(args[0]);
    return params;
  }

  protected Map<String, String> params;

  public ParameterMap() {
    params = new TreeMap<String, String>();
  }

  public ParameterMap(String parameterFile) {
    params = new TreeMap<String, String>();
    Pattern paramPattern = Pattern.compile("^(.*?):(.*)$");
    Pattern defaultPattern = Pattern.compile("^(.*)\\[(.*)\\]$");
    try {
      parameterFile = replaceVariables(parameterFile);
      BufferedReader in = new BufferedReader(new FileReader(parameterFile));
      String line = null;
      while ((line = in.readLine()) != null) {
        line = line.trim();
        if (line.length() == 0) {
          continue;
        }
        if (line.charAt(0) == ';') {
          continue;
        }
        Matcher matcher = paramPattern.matcher(line);
        if (matcher.find()) {
          String name = matcher.group(1).trim();
          String value = matcher.group(2).trim();
          String defaultValue = "";
          matcher = defaultPattern.matcher(name);
          if (matcher.find()) {
            name = matcher.group(1).trim();
            defaultValue = matcher.group(2).trim();
          }
          if (value.equals("")) {
            value = defaultValue;
          }
          value = replaceVariables(value);
          params.put(name, value);
        }
      }
      in.close();
    } catch (FileNotFoundException e) {
      ExceptionHandler.handle(e);
    } catch (IOException e) {
      ExceptionHandler.handle(e);
    }
  }

  private String replaceVariables(String value) {
    Pattern varPattern = Pattern.compile("\\+([a-zA-Z][a-zA-Z0-9_-]+)\\+");
    Matcher m = varPattern.matcher(value);
    StringBuffer sb = new StringBuffer();
    while (m.find()) {
      String varName = m.group(1);
      String replacement = staticVariables.get(varName);
      if (replacement == null) {
        throw new RuntimeException("Unable to replace variable name in parameter file: "+m.group(0));
      }
      else {
        m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
      }
    }
    m.appendTail(sb);
    return sb.toString();
  }

  public boolean getBooleanRequired(String field) {
    String str = getStringRequired(field);
    return parseBoolean(str);
  }

  public double getDoubleRequired(String field) {
    String str = getStringRequired(field);
    double value = 0.0;
    try {
      value = Double.parseDouble(str);
    } catch (NumberFormatException e) {
      value = 0.0;
    }
    return value;
  }

  public int getIntRequired(String field) {
    String str = getStringRequired(field);
    int value = 0;
    try {
      value = Integer.parseInt(str);
    } catch (NumberFormatException e) {
      value = 0;
    }
    return value;
  }

  public String[] getStringArrayRequired(String field) {
    String str = getStringRequired(field);
    String[] items = splitter.split(str.trim());
    return items;
  }

  public String getStringRequired(String field) {
    String str = params.get(field);
    if (str == null || str.equals("")) {
      throw new RuntimeException("Required field not defined: " + field);
    }
    return str;
  }

  public String getString(String field) {
    String str = params.get(field);
    return str;
  }

  public boolean hasParam(String field) {
    return params.containsKey(field);
  }

  public boolean getBoolean(String field) {
    String str = getString(field);
    return parseBoolean(str);
  }

  private boolean parseBoolean(String str) {
    if (str == null || str.equals("") || str.equals("0") || str.equals("false") || str.equals("no")) {
      return false;
    }

    return true;
  }

  public void put(String key, String value) {
    params.put(key, value);
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, String> e : params.entrySet()) {
      String key = e.getKey();
      String value = e.getValue();
      sb.append(key+": "+value+"\n");
    }
    return sb.toString();
  }
}
