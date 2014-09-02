package edu.isi.madcat.tmem.sql;

import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.sql.SQLException;

import edu.isi.madcat.tmem.logging.ExceptionHandler;

public class SqlUtils {
  public static String getString(ResultSet rs, int index) throws SQLException {
    String str = null;
    try {
      byte[] bytes = rs.getBytes(index);
      if (bytes != null) {
        str = new String(rs.getBytes(index), "utf-8");
      }
    } catch (UnsupportedEncodingException e) {
      ExceptionHandler.handle(e);
    }
    return str;
  }
}
