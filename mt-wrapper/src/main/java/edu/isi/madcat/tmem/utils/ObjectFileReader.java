package edu.isi.madcat.tmem.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;

import org.apache.commons.io.IOUtils;

import edu.isi.madcat.tmem.logging.ExceptionHandler;

public class ObjectFileReader {
  private ObjectInputStream inFp;

  public ObjectFileReader(String inputFile) {
    try {
      inFp =
          new ObjectInputStream(new BufferedInputStream(new FileInputStream(new File(inputFile))));
    } catch (FileNotFoundException e) {
      ExceptionHandler.handle(e);
    } catch (IOException e) {
      ExceptionHandler.handle(e);
    }
  }

  @SuppressWarnings("unchecked")
  public <T> T read() {
    T object = null;
    try {
      object = (T)inFp.readObject();
    } catch (IOException e) {
      ExceptionHandler.handle(e);
    } catch (ClassNotFoundException e) {
      ExceptionHandler.handle(e);
    }
    return object;
  }
  
  @SuppressWarnings("unchecked")
  public static <T> T read(String inputFile) {
    ObjectFileReader reader = new ObjectFileReader(inputFile);
    T object = (T)reader.read();
    reader.close();
    return object;
  }

  public void close() {
    if (inFp != null) {
      IOUtils.closeQuietly(inFp);
      inFp = null;
    }
  }
}
