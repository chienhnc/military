package com.military.repository.dynamodb.item;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@DynamoDbBean
public class SubmissionFlowGroupItem {
  private Integer orderNo;
  private Long groupId;

  public Integer getOrderNo() {
    return orderNo;
  }

  public void setOrderNo(Integer orderNo) {
    this.orderNo = orderNo;
  }

  public Long getGroupId() {
    return groupId;
  }

  public void setGroupId(Long groupId) {
    this.groupId = groupId;
  }
}
