package edu.isi.madcat.tmem.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import edu.isi.madcat.tmem.logging.ExceptionHandler;

public class TextSegmentIterator {
  protected String[] inputFiles;
  protected BufferedReader reader;
  protected TextSegment segment;
  protected int fileIndex;

  public TextSegmentIterator(String[] inputFiles) {
    this.inputFiles = inputFiles;
    this.segment = null;
    this.reader = null;
    this.fileIndex = 0;
  }

  public TextSegmentIterator(String inputFile) {
    this.inputFiles = new String[1];
    this.inputFiles[0] = inputFile;
    this.segment = null;
    this.reader = null;
    this.fileIndex = 0;
  }

  protected TextSegment readNextSegment() {
    while (true) {
      if (fileIndex >= inputFiles.length) {
        return null;
      }
      if (reader == null) {
        reader = new BufferedReader(new InputStreamReader(Utils.openFile(inputFiles[fileIndex])));
      }
      TextSegment segment = TextSegment.read(reader);
      if (segment != null) {
        return segment;
      } else {
        try {
          reader.close();
        } catch (IOException e) {
          ExceptionHandler.handle(e);
        }
        reader = null;
        fileIndex++;
      }
    }
  }

  public void close() {
    if (reader != null) {
      try {
        reader.close();
      } catch (IOException e) {
        ExceptionHandler.handle(e);
      }
    }
  }

  public TextSegment next() {
    TextSegment segment = readNextSegment();
    return segment;
  }
}
