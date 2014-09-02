package edu.isi.madcat.tmem.lookup;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Base64;

import edu.isi.madcat.tmem.logging.ExceptionHandler;

public class QueryHash {
  public static final int STRING_LENGTH;

  static {
    STRING_LENGTH = 16;
  }

  public static String getHashString(String str) {
    MessageDigest md = null;
    try {
      md = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      ExceptionHandler.handle(e);
    }
    md.update(str.getBytes());
    byte[] bytes = md.digest();
    String base64bytes = Base64.encodeBase64String(bytes);
    if (base64bytes.length() > STRING_LENGTH) {
      return base64bytes.substring(0, STRING_LENGTH);
    } else {
      StringBuffer sb = new StringBuffer();
      sb.append(base64bytes);
      for (int i = base64bytes.length(); i < STRING_LENGTH; i++) {
        sb.append("=");
      }
      return sb.toString();
    }
  }
}
