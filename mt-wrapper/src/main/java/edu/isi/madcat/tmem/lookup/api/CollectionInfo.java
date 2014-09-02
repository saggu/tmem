package edu.isi.madcat.tmem.lookup.api;

public class CollectionInfo {
  public static final int TYPE_DICTIONARY = 0;
  public static final int TYPE_CORPUS = 1;
  public static final int TYPE_USER = 2;

  public static int parseCollectionType(String type) {
    if (type.equals("DICTIONARY")) {
      return TYPE_DICTIONARY;
    } else if (type.equals("CORPUS")) {
      return TYPE_CORPUS;
    } else if (type.equals("TYPE_USER")) {
      return TYPE_USER;
    }
    throw new RuntimeException("Unknown collection type: " + type);
  }

  private int collectionType;
  private String shortName;
  private String fullName;

  public CollectionInfo(int collectionType, String shortName, String fullName) {
    super();
    this.collectionType = collectionType;
    this.shortName = shortName;
    this.fullName = fullName;
  }

  public int getCollectionType() {
    return collectionType;
  }

  public String getFullName() {
    return fullName;
  }

  public String getShortName() {
    return shortName;
  }

  public void setCollectionType(int collectionType) {
    this.collectionType = collectionType;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public void setShortName(String shortName) {
    this.shortName = shortName;
  }

}
