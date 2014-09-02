package edu.isi.madcat.tmem.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import org.apache.commons.io.IOUtils;

import edu.isi.madcat.tmem.logging.ExceptionHandler;

public class ObjectFileWriter {
  private ObjectOutputStream outFp;

  public ObjectFileWriter(String outputFile) {
    try {
      outFp =
          new ObjectOutputStream(new BufferedOutputStream(
              new FileOutputStream(new File(outputFile))));
    } catch (FileNotFoundException e) {
      ExceptionHandler.handle(e);
    } catch (IOException e) {
      ExceptionHandler.handle(e);
    }
  }

  public void close() {
    try {
      outFp.writeObject(null);
    } catch (IOException e) {
      ExceptionHandler.handle(e);
    }
    if (outFp != null) {
      IOUtils.closeQuietly(outFp);
      outFp = null;
    }
  }

  public void write(Object object) {
    if (object == null) {
      throw new RuntimeException("Cannot serialize an object of type null");
    }
    try {
      outFp.writeObject(object);
    } catch (IOException e) {
      ExceptionHandler.handle(e);
    }
  }

  public static void write(Object object, String outputFile) {
    ObjectFileWriter writer = new ObjectFileWriter(outputFile);
    writer.write(object);
    writer.close();
  }
}
