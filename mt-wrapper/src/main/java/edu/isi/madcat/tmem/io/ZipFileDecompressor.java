package edu.isi.madcat.tmem.io;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import edu.isi.madcat.tmem.exceptions.CateProcessException;
import edu.isi.madcat.tmem.logging.ExceptionHandler;

public class ZipFileDecompressor extends FileDecompressor {
  public ZipFileDecompressor() {

  }

  @Override
  public StoredFileSet decompress(InputStream is) throws CateProcessException {
    StoredFileSet storedFileSet = null;
    try {
      storedFileSet = new StoredFileSet();
      int bufferSize = 8192;
      ByteArrayOutputStream dest = null;
      ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is));
      ZipEntry entry = null;
      while ((entry = zis.getNextEntry()) != null) {
        String name = entry.getName();
        int count = 0;
        byte data[] = new byte[bufferSize];
        dest = new ByteArrayOutputStream();
        while ((count = zis.read(data, 0, bufferSize)) != -1) {
          dest.write(data, 0, count);
        }
        dest.flush();
        dest.close();
        StoredFile storedFile = new StoredFile(name, dest.toByteArray());
        storedFileSet.addFile(storedFile);
      }
      zis.close();
    } catch (IOException e) {
      ExceptionHandler.handle(e);
    }
    return storedFileSet;
  }
}
