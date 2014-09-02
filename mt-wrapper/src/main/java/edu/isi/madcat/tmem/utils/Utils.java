package edu.isi.madcat.tmem.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.BeanFactory;

import edu.isi.madcat.tmem.alignment.Range;
import edu.isi.madcat.tmem.logging.ExceptionHandler;

public class Utils {
  protected static Pattern baseFileRegex;
  
  static {
    baseFileRegex = Pattern.compile("^(.*)[\\\\/](.*)$");
  }

  public static void debug(String message) {
    System.out.println(message);
  }

  public static String processWhitespace(String input) {
    String output = StringUtils.strip(input);
    output = output.replaceAll("\\s+", " ");
    return output;
  }
  
  public static String getBaseFile(String filename) {
    Matcher matcher = baseFileRegex.matcher(filename);
    if (matcher.find()) {
      return matcher.group(2);
    }
    return null;
  }

  public static Object getRequiredBean(BeanFactory factory, String name) {
    Object obj = factory.getBean(name);
    if (obj == null) {
      throw new RuntimeException("Required bean is not specified: " + name);
    }
    return obj;
  }

  public static String getStackTrace(Throwable e) {
    StringWriter writer = new StringWriter();
    e.printStackTrace(new PrintWriter(writer));
    return writer.toString();
  }

  public static String objectReference(Object object) {
    return ObjectUtils.identityToString(object);
  }

  public static InputStream openFile(String fileName) {
    InputStream is = null;
    try {
      is = new GzipCompressorInputStream(new FileInputStream(fileName));
    } catch (IOException e) {
      IOUtils.closeQuietly(is);
      is = null;
    }
    if (is == null) {
      try {
        is = new FileInputStream(fileName);
      } catch (FileNotFoundException e) {
        ExceptionHandler.handle(e);
      }
    }
    return is;
  }

  public static List<String> readLines(BufferedReader reader) {
    List<String> lines = new ArrayList<String>();
    try {
      String line = null;
      while ((line = reader.readLine()) != null) {
        lines.add(line);
      }
      reader.close();
    } catch (FileNotFoundException e) {
      ExceptionHandler.handle(e);
    } catch (IOException e) {
      ExceptionHandler.handle(e);
    }
    return lines;
  }
  
  public static List<String> readLines(InputStream is) {
    List<String> lines = null;
    try {
      lines = readLines(new BufferedReader(new InputStreamReader(is, "utf-8")));
    } catch (UnsupportedEncodingException e) {
      ExceptionHandler.handle(e);
    }
    return lines;
  }

  public static List<String> readLinesFromFile(String inputFile) {
    InputStream is = openFile(inputFile);
    List<String> lines = readLines(is);
    IOUtils.closeQuietly(is);
    return lines;
  }

  public static List<Map.Entry<Range, Range>> setToList(Set<Map.Entry<Range, Range>> set) {
    List<Map.Entry<Range, Range>> list = new ArrayList<Map.Entry<Range, Range>>();
    for (Map.Entry<Range, Range> s : set) {
      list.add(s);
    }
    return list;
  }

  public static Set<String> readFileToSet(String inputFile) {
    List<String> lines = null;
    try {
      lines = FileUtils.readLines(new File(inputFile), "utf-8");
    } catch (IOException e) {
      ExceptionHandler.handle(e);
    }
    Set<String> lineSet = new HashSet<String>();
    for (String line : lines) {
      lineSet.add(line);
    }
    return lineSet;
  }
  
  public static Writer createWriter(String outputFile) {
    Writer writer = null;
    try {
      writer = new OutputStreamWriter(
          new GZIPOutputStream(new FileOutputStream(
              outputFile)));
    } catch (FileNotFoundException e) {
      ExceptionHandler.handle(e);
    } catch (IOException e) {
      ExceptionHandler.handle(e);
    }
    return writer;
  }

  public static List<String> trimAndSplit(String str) {
    str = StringUtils.trim(str);
    String[] array = str.split("\\s+");
    List<String> list = Arrays.asList(array);
    return list;
  }  
}
