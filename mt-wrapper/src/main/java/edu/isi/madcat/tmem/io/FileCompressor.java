package edu.isi.madcat.tmem.io;

import java.io.OutputStream;

public abstract class FileCompressor {
  public abstract void compress(StoredFileSet storedFileSet, OutputStream is);
}
