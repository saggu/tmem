package edu.isi.madcat.tmem.lextrans;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;

import edu.isi.madcat.tmem.logging.ExceptionHandler;

public class SerializedRule implements java.io.Serializable, Comparable<SerializedRule> {
  @Override
  public String toString() {
    return "SerializedRule [key=" + key + ", serializedRule=" + Arrays.toString(serializedRule)
        + "]";
  }

  public int compareTo(SerializedRule other) {
    return getKey().compareTo(other.getKey());
  }

  static final long serialVersionUID = 1L;

  private String key;

  private byte[] serializedRule;

  public SerializedRule() {
    super();
    this.key = null;
    this.serializedRule = null;
  }

  public SerializedRule(TranslationRule rule) {
    super();
    key = rule.getKey();
    try {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      ObjectOutputStream out = new ObjectOutputStream(bos);
      out.writeObject(rule);
      serializedRule = bos.toByteArray();
    } catch (IOException e) {
      ExceptionHandler.handle(e);
    }
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public TranslationRule unserializeRule() {
    TranslationRule rule = null;
    try {
      ByteArrayInputStream bis = new ByteArrayInputStream(serializedRule);
      ObjectInputStream in = new ObjectInputStream(bis);
      rule = (TranslationRule) in.readObject();
      rule.initialize();
      in.close();
    } catch (ClassNotFoundException e) {
      ExceptionHandler.handle(e);
    } catch (IOException e) {
      ExceptionHandler.handle(e);
    }
    return rule;
  }
}
