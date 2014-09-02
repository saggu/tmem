package edu.isi.madcat.tmem.sql;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import edu.isi.madcat.tmem.logging.ExceptionHandler;

public class InfileWriter {
  private OutputStream writer;
  private int maxLength;
  private int numRows;

  public InfileWriter(String filename) {
    try {
      this.writer = new FileOutputStream(new File(filename));
    } catch (IOException e) {
      ExceptionHandler.handle(e);
    }
    this.maxLength = -1;
    this.numRows = 0;
  }

  public InfileWriter(String filename, int maxLength) {
    try {
      this.writer = new FileOutputStream(new File(filename));
    } catch (IOException e) {
      ExceptionHandler.handle(e);
    }
    this.maxLength = maxLength;
    this.numRows = 0;
  }

  public static void writeAll(List<List<String>> rows, String outputFileName, int maxLength) {
    InfileWriter infileWriter = new InfileWriter(outputFileName, maxLength);
    for (List<String> row : rows) {
      infileWriter.write(row);
    }
    infileWriter.close();
  }

  public void write(List<String> items) {
    try {
      for (int i = 0; i < items.size(); i++) {
        String item = items.get(i);
        if (i > 0) {
          writer.write(new String("\t").getBytes());
        }
        int numBytes = 0;
        for (int j = 0; j < item.length() && (maxLength == -1 || numBytes < maxLength); j++) {
          char c = item.charAt(j);
          String str = "" + c;
          if (c == '\0') {
            str = "\\0";
          } else if (c == '\b') {
            str = "\\b";
          } else if (c == '\\') {
            str = "\\\\";
          } else if (c == '\n') {
            str = "\\n";
          } else if (c == '\r') {
            str = "\\r";
          } else if (c == '\t') {
            str = "\\t";
          } else if (c == (char) 26) {
            str = "\\Z";
          }
          byte[] bytes = str.getBytes();
          numBytes += bytes.length;
          writer.write(bytes);
        }
      }
      writer.write(new String("\n").getBytes());
    } catch (IOException e) {
      ExceptionHandler.handle(e);
    }
    numRows++;
  }

  public void close() {
    try {
      writer.close();
    } catch (IOException e) {
      ExceptionHandler.handle(e);
    }
  }

  public int getNumRows() {
    return numRows;
  }

  public static void writeFile(SqlManager sqlManager, String inputFile, String tableName) {
    SqlHandler sqlHandler = sqlManager.createHandler();
    System.out.println("Writing file " + inputFile + " to SQL table " + tableName);
    sqlHandler.executeStatement("LOAD DATA INFILE '" + inputFile + "' INTO TABLE " + tableName);
    sqlHandler.close();
  }

  public static void writeSplitFile(SqlManager sqlManager, String inputFile, String tableName,
      int maxItems) {
    SqlHandler sqlHandler = sqlManager.createHandler();
    System.out.println("Splitting csv file "+inputFile);
    List<String> filenames = splitFile(inputFile, maxItems);
    for (int i = 0; i < filenames.size(); i++) {
      String filename = filenames.get(i);
      System.out.println("Writing file " + filename + " to SQL table " + tableName);
      sqlHandler.executeStatement("LOAD DATA INFILE '" + filename + "' INTO TABLE " + tableName);
    }
    sqlHandler.close();
  }

  private static List<String> splitFile(String inputFile, int maxItems) {
    List<String> filenames = new ArrayList<String>();
    try {
      int lineCount = 0;
      RandomAccessFile reader = new RandomAccessFile(inputFile, "r");
      while (reader.readLine() != null) {
        lineCount++;
      }
      int numFiles = lineCount / maxItems;
      if (lineCount % maxItems != 0) {
        numFiles++;
      }
      reader.seek(0);
      int k = 0;
      for (int i = 0; i < numFiles; i++) {
        String outputFile = inputFile + ".split_" + i;
        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(outputFile)));
        for (int j = 0; j < maxItems && k < lineCount; j++, k++) {
          String line = reader.readLine();
          writer.write(line + "\n");
        }
        filenames.add(outputFile);
        writer.close();
      }
      reader.close();
    } catch (IOException e) {
      ExceptionHandler.handle(e);
    }
    return filenames;
  }
}
