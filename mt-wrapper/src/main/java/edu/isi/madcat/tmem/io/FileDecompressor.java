package edu.isi.madcat.tmem.io;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import edu.isi.madcat.tmem.exceptions.CateProcessException;
import edu.isi.madcat.tmem.logging.ExceptionHandler;

public abstract class FileDecompressor {
  public abstract StoredFileSet decompress(InputStream is) throws CateProcessException;

  public StoredFileSet decompress(String filename) throws CateProcessException {
    StoredFileSet fileSet = null;
    try {
      InputStream is = new BufferedInputStream(new FileInputStream(filename));
      fileSet = decompress(is);
      is.close();
    } catch (FileNotFoundException e) {
      ExceptionHandler.handle(e);
    } catch (IOException e) {
      ExceptionHandler.handle(e);
    }
    return fileSet;
  }
}
