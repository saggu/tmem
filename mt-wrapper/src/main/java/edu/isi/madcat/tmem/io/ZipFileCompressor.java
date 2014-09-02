package edu.isi.madcat.tmem.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import edu.isi.madcat.tmem.logging.ExceptionHandler;

public class ZipFileCompressor extends FileCompressor {

  @Override
  public void compress(StoredFileSet storedFileSet, OutputStream os) {
    try {
      int bufferSize = 8192;
      byte data[] = new byte[bufferSize];
      ZipOutputStream out = new ZipOutputStream(os);
      for (StoredFile file : storedFileSet) {
        InputStream is = file.createInputStream();
        ZipEntry entry = new ZipEntry(file.getFilename());
        out.putNextEntry(entry);
        int count = 0;
        while ((count = is.read(data, 0, bufferSize)) != -1) {
          out.write(data, 0, count);
        }
        is.close();
      }
      out.close();
    } catch (IOException e) {
      ExceptionHandler.handle(e);
    }
  }
}
