package com.military.repository.dynamodb.item;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.time.LocalDate;

@DynamoDbBean
public class QrScanLogItem {
  private Long id;
  private String scanType;
  private String scannedAt;
  private String status;
  private String reason;

  private Long militaryPersonnelId;
  private String militaryPersonnelCode;
  private String militaryPersonnelFullName;

  private String citizenName;
  private LocalDate citizenBirthday;
  private String citizenAddress;
  private String citizenId;
  private LocalDate citizenIssueDate;

  private Long leaveRequestId;
  private String approvedRoundNo;
  private String payloadJson;

  @DynamoDbPartitionKey
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getScanType() {
    return scanType;
  }

  public void setScanType(String scanType) {
    this.scanType = scanType;
  }

  public String getScannedAt() {
    return scannedAt;
  }

  public void setScannedAt(String scannedAt) {
    this.scannedAt = scannedAt;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public Long getMilitaryPersonnelId() {
    return militaryPersonnelId;
  }

  public void setMilitaryPersonnelId(Long militaryPersonnelId) {
    this.militaryPersonnelId = militaryPersonnelId;
  }

  public String getMilitaryPersonnelCode() {
    return militaryPersonnelCode;
  }

  public void setMilitaryPersonnelCode(String militaryPersonnelCode) {
    this.militaryPersonnelCode = militaryPersonnelCode;
  }

  public String getMilitaryPersonnelFullName() {
    return militaryPersonnelFullName;
  }

  public void setMilitaryPersonnelFullName(String militaryPersonnelFullName) {
    this.militaryPersonnelFullName = militaryPersonnelFullName;
  }

  public String getCitizenName() {
    return citizenName;
  }

  public void setCitizenName(String citizenName) {
    this.citizenName = citizenName;
  }

  public LocalDate getCitizenBirthday() {
    return citizenBirthday;
  }

  public void setCitizenBirthday(LocalDate citizenBirthday) {
    this.citizenBirthday = citizenBirthday;
  }

  public String getCitizenAddress() {
    return citizenAddress;
  }

  public void setCitizenAddress(String citizenAddress) {
    this.citizenAddress = citizenAddress;
  }

  public String getCitizenId() {
    return citizenId;
  }

  public void setCitizenId(String citizenId) {
    this.citizenId = citizenId;
  }

  public LocalDate getCitizenIssueDate() {
    return citizenIssueDate;
  }

  public void setCitizenIssueDate(LocalDate citizenIssueDate) {
    this.citizenIssueDate = citizenIssueDate;
  }

  public Long getLeaveRequestId() {
    return leaveRequestId;
  }

  public void setLeaveRequestId(Long leaveRequestId) {
    this.leaveRequestId = leaveRequestId;
  }

  public String getApprovedRoundNo() {
    return approvedRoundNo;
  }

  public void setApprovedRoundNo(String approvedRoundNo) {
    this.approvedRoundNo = approvedRoundNo;
  }

  public String getPayloadJson() {
    return payloadJson;
  }

  public void setPayloadJson(String payloadJson) {
    this.payloadJson = payloadJson;
  }
}
