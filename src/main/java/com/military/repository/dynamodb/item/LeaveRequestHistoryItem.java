package com.military.repository.dynamodb.item;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.time.LocalDate;

@DynamoDbBean
public class LeaveRequestHistoryItem {
  private Long id;
  private Long leaveRequestId;
  private String roundNo;
  private Long militaryPersonnelId;
  private Long userId;
  private String createdAt;
  private LocalDate leaveFrom;
  private LocalDate leaveTo;
  private String status;
  private String assignee;
  private Long flowId;
  private Integer orderNo;
  private String reason;

  @DynamoDbPartitionKey
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getLeaveRequestId() {
    return leaveRequestId;
  }

  public void setLeaveRequestId(Long leaveRequestId) {
    this.leaveRequestId = leaveRequestId;
  }

  public String getRoundNo() {
    return roundNo;
  }

  public void setRoundNo(String roundNo) {
    this.roundNo = roundNo;
  }

  public Long getMilitaryPersonnelId() {
    return militaryPersonnelId;
  }

  public void setMilitaryPersonnelId(Long militaryPersonnelId) {
    this.militaryPersonnelId = militaryPersonnelId;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public String getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(String createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDate getLeaveFrom() {
    return leaveFrom;
  }

  public void setLeaveFrom(LocalDate leaveFrom) {
    this.leaveFrom = leaveFrom;
  }

  public LocalDate getLeaveTo() {
    return leaveTo;
  }

  public void setLeaveTo(LocalDate leaveTo) {
    this.leaveTo = leaveTo;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getAssignee() {
    return assignee;
  }

  public void setAssignee(String assignee) {
    this.assignee = assignee;
  }

  public Long getFlowId() {
    return flowId;
  }

  public void setFlowId(Long flowId) {
    this.flowId = flowId;
  }

  public Integer getOrderNo() {
    return orderNo;
  }

  public void setOrderNo(Integer orderNo) {
    this.orderNo = orderNo;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }
}
