package com.military.repository.dynamodb.item;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.time.LocalDate;

@DynamoDbBean
public class LeaveRequestItem {
  private Long id;
  private Long militaryPersonnelId;
  private Long userId;
  private String createdAt;
  private LocalDate leaveFrom;
  private LocalDate leaveTo;
  private String status;
  private Long flowId;
  private Integer currentOrderNo;
  private String currentRound;
  private String currentAssignee;
  private String reason;

  @DynamoDbPartitionKey
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
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

  public Long getFlowId() {
    return flowId;
  }

  public void setFlowId(Long flowId) {
    this.flowId = flowId;
  }

  public Integer getCurrentOrderNo() {
    return currentOrderNo;
  }

  public void setCurrentOrderNo(Integer currentOrderNo) {
    this.currentOrderNo = currentOrderNo;
  }

  public String getCurrentRound() {
    return currentRound;
  }

  public void setCurrentRound(String currentRound) {
    this.currentRound = currentRound;
  }

  public String getCurrentAssignee() {
    return currentAssignee;
  }

  public void setCurrentAssignee(String currentAssignee) {
    this.currentAssignee = currentAssignee;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }
}
