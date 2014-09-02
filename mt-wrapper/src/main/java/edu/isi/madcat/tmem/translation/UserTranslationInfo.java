package edu.isi.madcat.tmem.translation;

import java.util.ArrayList;
import java.util.List;

public class UserTranslationInfo {
  private List<Integer> userIds;

  private List<Integer> groupIds;

  public UserTranslationInfo() {
    this.userIds = new ArrayList<Integer>();
    this.groupIds = new ArrayList<Integer>();
  }
  
  public UserTranslationInfo(int userId, int groupId) {
    super();
    this.userIds = new ArrayList<Integer>();
    this.userIds.add(userId);
    this.groupIds = new ArrayList<Integer>();
    this.groupIds.add(groupId);
  }

  public UserTranslationInfo(List<Integer> userIds, List<Integer> groupIds) {
    super();
    this.userIds = userIds;
    this.groupIds = groupIds;
  }

  public List<Integer> getGroupIds() {
    return groupIds;
  }

  public List<Integer> getUserIds() {
    return userIds;
  }

  public void setGroupIds(List<Integer> groupIds) {
    this.groupIds = groupIds;
  }

  public void setUserIds(List<Integer> userIds) {
    this.userIds = userIds;
  }
}
