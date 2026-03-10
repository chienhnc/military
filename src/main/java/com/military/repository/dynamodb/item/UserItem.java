package com.military.repository.dynamodb.item;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.util.HashSet;
import java.util.Set;

@DynamoDbBean
public class UserItem {
  private Long id;
  private String username;
  private String email;
  private String password;
  private Set<String> roleNames = new HashSet<>();
  private Long militaryPersonnelId;

  @DynamoDbPartitionKey
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public Set<String> getRoleNames() {
    return roleNames;
  }

  public void setRoleNames(Set<String> roleNames) {
    this.roleNames = roleNames;
  }

  public Long getMilitaryPersonnelId() {
    return militaryPersonnelId;
  }

  public void setMilitaryPersonnelId(Long militaryPersonnelId) {
    this.militaryPersonnelId = militaryPersonnelId;
  }
}
