package edu.isi.madcat.tmem.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import edu.isi.madcat.tmem.exceptions.CateProcessException;

public class StoredFile {
  public static InputStream fileToByteStream(String filename) throws CateProcessException {
    return StoredFile.readFile(filename).createInputStream();
  }

  public static StoredFile readFile(String filename) throws CateProcessException {
    int bufferSize = 8192;
    StoredFile file = null;
    try {
      BufferedInputStream is = new BufferedInputStream(new FileInputStream(filename));
      int count = 0;
      byte data[] = new byte[bufferSize];
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      while ((count = is.read(data, 0, bufferSize)) != -1) {
        os.write(data, 0, count);
      }
      file = new StoredFile(filename, os.toByteArray());
      is.close();
      os.close();
    } catch (FileNotFoundException e) {
      throw new CateProcessException("File not found: " + filename);
    } catch (IOException e) {
      throw new CateProcessException("Unable to read from file: " + filename);
    }
    return file;
  }

  public static StoredFile write(byte[] bytes, String filename) throws CateProcessException {
    int bufferSize = 8192;
    StoredFile file = null;
    try {
      BufferedInputStream is = new BufferedInputStream(new ByteArrayInputStream(bytes));
      BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(filename));
      byte data[] = new byte[bufferSize];
      int count = 0;
      while ((count = is.read(data, 0, bufferSize)) != -1) {
        os.write(data, 0, count);
      }
      is.close();
      os.close();
    } catch (FileNotFoundException e) {
      throw new CateProcessException("File not found: " + filename);
    } catch (IOException e) {
      throw new CateProcessException("Unable to read from file: " + filename);
    }
    return file;
  }

  protected byte[] contents;

  protected String filename;

  public StoredFile(String filename, byte[] contents) {
    super();
    this.filename = filename;
    this.contents = contents;
  }
  
  public StoredFile(String filename) {
    super();
    this.filename = filename;
    this.contents = new byte[0];
  }

  public InputStream createInputStream() {
    return new BufferedInputStream(new ByteArrayInputStream(contents));
  }

  public byte[] getContents() {
    return contents;
  }

  public String getFilename() {
    return filename;
  }

  public void setContents(byte[] contents) {
    this.contents = contents;
  }

  public void write() throws CateProcessException {
    StoredFile.write(this.contents, this.filename);
  }
}
