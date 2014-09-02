package edu.isi.madcat.tmem.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import edu.isi.madcat.tmem.logging.ExceptionHandler;

public class SqlHandler {
  private String url;
  private String user;
  private String password;
  private Connection con;

  public SqlHandler(String url, String user, String password) {
    super();
    this.url = url;
    this.user = user;
    this.password = password;
    try {
      Class.forName("com.mysql.jdbc.Driver");
      this.con = DriverManager.getConnection(this.url, this.user, this.password);
    } catch (ClassNotFoundException e) {
      ExceptionHandler.handle(e);
    } catch (SQLException e) {
      ExceptionHandler.handle(e);
    }
  }

  public Connection getConnection() {
    return con;
  }

  public PreparedStatement prepareStatement(String statementString) {
    PreparedStatement statement = null;
    try {
      statement = con.prepareStatement(statementString);
    } catch (SQLException e) {
      ExceptionHandler.handle(e);
    }
    return statement;
  }

  public void close() {
    try {
      if (con != null) {
        con.close();
      }
      con = null;
    } catch (SQLException e) {
      ExceptionHandler.handle(e);
    }
  }

  public void executeStatement(String statementString) {
    PreparedStatement statement = prepareStatement(statementString);
    try {
      statement.execute();
    } catch (SQLException e) {
      ExceptionHandler.handle(e);
    }
  }

  public int getNextId(String tableName, String columnName) {
    List<List<String>> results = executeQuery("SELECT MAX(" + columnName + ") FROM " + tableName);
    if (results.get(0).get(0) == null) {
      return 0;
    }
    return Integer.parseInt(results.get(0).get(0)) + 1;
  }

  public List<List<String>> executeQuery(PreparedStatement statement) {
    List<List<String>> results = new ArrayList<List<String>>();
    try {
      ResultSet rs = statement.executeQuery();
      ResultSetMetaData rsmd = rs.getMetaData();
      int numColumns = rsmd.getColumnCount();
      while (rs.next()) {
        List<String> row = new ArrayList<String>();
        for (int i = 1; i <= numColumns; i++) {
          String cell = SqlUtils.getString(rs, i);
          row.add(cell);
        }
        results.add(row);
      }
    } catch (SQLException e) {
      ExceptionHandler.handle(e);
    }
    return results;
  }

  public List<List<String>> executeQuery(String query) {
    PreparedStatement statement = prepareStatement(query);
    return executeQuery(statement);
  }

}
