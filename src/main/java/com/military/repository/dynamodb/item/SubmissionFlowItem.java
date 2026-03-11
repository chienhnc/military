package com.military.repository.dynamodb.item;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.util.ArrayList;
import java.util.List;

@DynamoDbBean
public class SubmissionFlowItem {
  private Long id;
  private String name;
  private String description;
  private List<SubmissionFlowGroupItem> groups = new ArrayList<>();

  @DynamoDbPartitionKey
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public List<SubmissionFlowGroupItem> getGroups() {
    return groups;
  }

  public void setGroups(List<SubmissionFlowGroupItem> groups) {
    this.groups = groups;
  }
}
