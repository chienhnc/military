package com.military.repository.dynamodb.item;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.time.LocalDate;

@DynamoDbBean
public class LeaveApprovalConfigItem {
  private Long id;
  private String militaryPosition;
  private Integer maxApprovalDays;
  private LocalDate effectiveFrom;
  private LocalDate effectiveTo;
  private Boolean active;

  @DynamoDbPartitionKey
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getMilitaryPosition() {
    return militaryPosition;
  }

  public void setMilitaryPosition(String militaryPosition) {
    this.militaryPosition = militaryPosition;
  }

  public Integer getMaxApprovalDays() {
    return maxApprovalDays;
  }

  public void setMaxApprovalDays(Integer maxApprovalDays) {
    this.maxApprovalDays = maxApprovalDays;
  }

  public LocalDate getEffectiveFrom() {
    return effectiveFrom;
  }

  public void setEffectiveFrom(LocalDate effectiveFrom) {
    this.effectiveFrom = effectiveFrom;
  }

  public LocalDate getEffectiveTo() {
    return effectiveTo;
  }

  public void setEffectiveTo(LocalDate effectiveTo) {
    this.effectiveTo = effectiveTo;
  }

  public Boolean getActive() {
    return active;
  }

  public void setActive(Boolean active) {
    this.active = active;
  }
}
