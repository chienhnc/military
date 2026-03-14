package com.military.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Du lieu QR cua nguoi dan")
public class QrCitizenDataRequest {
  private String name;
  private LocalDate birthday;
  private String address;
  private String citizenId;
  private LocalDate issueDate;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public LocalDate getBirthday() {
    return birthday;
  }

  public void setBirthday(LocalDate birthday) {
    this.birthday = birthday;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public String getCitizenId() {
    return citizenId;
  }

  public void setCitizenId(String citizenId) {
    this.citizenId = citizenId;
  }

  public LocalDate getIssueDate() {
    return issueDate;
  }

  public void setIssueDate(LocalDate issueDate) {
    this.issueDate = issueDate;
  }
}
