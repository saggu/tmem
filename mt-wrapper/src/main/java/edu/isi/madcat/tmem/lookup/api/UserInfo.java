package edu.isi.madcat.tmem.lookup.api;

public class UserInfo {
  private int userId;
  private int groupId;

  public UserInfo() {
    super();
  }
  
  public UserInfo(int userId, int groupId) {
    super();
    this.userId = userId;
    this.groupId = groupId;
  }

  public int getGroupId() {
    return groupId;
  }

  public int getUserId() {
    return userId;
  }

  public void setGroupId(int groupId) {
    this.groupId = groupId;
  }

  public void setUserId(int userId) {
    this.userId = userId;
  }
}
