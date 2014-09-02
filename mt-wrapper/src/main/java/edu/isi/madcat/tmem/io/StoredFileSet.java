package edu.isi.madcat.tmem.io;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

public class StoredFileSet implements Iterable<StoredFile> {
  private ArrayList<StoredFile> files;
  private AbstractMap<String, Integer> nameToFileIndex;

  public StoredFileSet() {
    super();
    files = new ArrayList<StoredFile>();
    nameToFileIndex = new TreeMap<String, Integer>();
  }

  public StoredFileSet(StoredFileSet rhs) {
    super();
    files = new ArrayList<StoredFile>(rhs.files);
    nameToFileIndex = new TreeMap<String, Integer>(rhs.nameToFileIndex);
  }

  public void addFile(StoredFile file) {
    Integer index = nameToFileIndex.get(file.getFilename());
    if (index == null) {
      index = new Integer(files.size());
      files.add(file);
      nameToFileIndex.put(file.getFilename(), index);
    } else {
      files.set(index, file);
    }
  }

  public StoredFile getFileByName(String filename) {
    Integer v = nameToFileIndex.get(filename);
    if (v == null) {
      return null;
    }
    return files.get(v.intValue());
  }

  public Iterator<StoredFile> iterator() {
    return files.listIterator();
  }
}
