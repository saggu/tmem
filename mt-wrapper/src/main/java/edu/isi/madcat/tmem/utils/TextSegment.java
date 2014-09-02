
package edu.isi.madcat.tmem.utils;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.text.translate.LookupTranslator;

import edu.isi.madcat.tmem.logging.ExceptionHandler;

public class TextSegment {
  protected static LookupTranslator escaper;
  protected static Pattern fieldPattern;
  protected static LookupTranslator unescaper;
  static {
    Map<String, String> charMap = new HashMap<String, String>();
    charMap.put("\"", "&quot;");
    charMap.put("'", "&apos;");
    charMap.put("<", "&lt;");
    charMap.put(">", "&gt;");
    charMap.put("\n", "&#10;");
    charMap.put("\r", "&#13;");
    charMap.put("&", "&amp;");

    String[][] escaperStrings = new String[charMap.size()][];
    String[][] unescaperStrings = new String[charMap.size()][];
    int i = 0;
    for (Map.Entry<String, String> entry : charMap.entrySet()) {
      escaperStrings[i] = new String[2];
      escaperStrings[i][0] = entry.getKey();
      escaperStrings[i][1] = entry.getValue();

      unescaperStrings[i] = new String[2];
      unescaperStrings[i][0] = entry.getValue();
      unescaperStrings[i][1] = entry.getKey();
      i++;
    }
    escaper = new LookupTranslator(escaperStrings);
    unescaper = new LookupTranslator(unescaperStrings);

    fieldPattern = Pattern.compile("<(.*?)>(.*)</(.*?)>", Pattern.UNIX_LINES);
  }

  public static TextSegment read(BufferedReader reader) {
    TextSegment segment = null;
    try {
      String line = null;
      line = reader.readLine();
      if (line == null) {
        return null;
      }
      segment = new TextSegment();
      line = line.trim();
      if (!line.equals("<SEGMENT>")) {
        throw new RuntimeException("Malformed segment file: Bad first line: " + line);
      }
      while (true) {
        line = reader.readLine();
        if (line == null) {
          throw new RuntimeException("Malformed segment file: No </SEGMENT> tag.");
        }
        line = line.trim();
        if (line.equals("</SEGMENT>")) {
          break;
        }
        Matcher matcher = fieldPattern.matcher(line);
        if (matcher.find()) {
          String fieldName1 = matcher.group(1);
          String value = unescape(matcher.group(2));
          String fieldName2 = matcher.group(3);
          if (!(fieldName1.equals(fieldName2))) {
            throw new RuntimeException("Malformed line in segment file: " + line);
          }
          segment.insert(fieldName1, value);
        } else {
          throw new RuntimeException("Malformed line in segment file: " + line);
        }
      }
    } catch (IOException e) {
      ExceptionHandler.handle(e);
    }
    return segment;
  }

  public static TextSegment read(InputStream is) {
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new InputStreamReader(is, "utf-8"));
    } catch (UnsupportedEncodingException e) {
      ExceptionHandler.handle(e);
    }
    return read(reader);
  }

  public static List<TextSegment> readSegments(String filename) {
    List<TextSegment> segments = new ArrayList<TextSegment>();
    readSegments(filename, segments);
    return segments;
  }

  public static List<TextSegment> readSegments(String[] filenames) {
    List<TextSegment> segments = new ArrayList<TextSegment>();
    for (String filename : filenames) {
      readSegments(filename, segments);
    }
    return segments;
  }

  protected static String escape(String input) {
    return escaper.translate(input);
  }

  protected static void readSegments(String filename, List<TextSegment> segments) {
    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(Utils.openFile(filename)));
      TextSegment segment = null;
      while ((segment = TextSegment.read(reader)) != null) {
        segments.add(segment);
      }
      reader.close();
    } catch (FileNotFoundException e) {
      ExceptionHandler.handle(e);
    } catch (IOException e) {
      ExceptionHandler.handle(e);
    }
  }

  protected static String unescape(String input) {
    return unescaper.translate(input);
  }

  protected Map<String, String> fields;

  public TextSegment() {
    fields = new TreeMap<String, String>();
  }

  public TextSegment(TextSegment other) {
    fields = new TreeMap<String, String>(other.fields);
  }

  public String get(String field) {
    return fields.get(field);
  }
  
  public boolean contains(String key) {
    return fields.containsKey(key);
  }
  
  public String getRequired(String field) {
    String value = get(field);
    if (value == null) {
      throw new RuntimeException("Required field not found: " + field);
    }
    return value;
  }

  public void insert(String field, String value) {
    fields.put(field, value);
  }

  public void write(Writer writer) {
    try {
      writer.write("<SEGMENT>\n");
      for (Map.Entry<String, String> item : fields.entrySet()) {
        writer.write("    ");
        writer.write("<" + item.getKey() + ">");
        writer.write(escape(item.getValue()));
        writer.write("</" + item.getKey() + ">");
        writer.write("\n");
      }
      writer.write("</SEGMENT>\n");
    } catch (IOException e) {
      ExceptionHandler.handle(e);
    }
  }
}
