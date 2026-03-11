package com.military.repository.dynamodb.item;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.time.LocalDate;

@DynamoDbBean
public class MilitaryUnitItem {
  private Long id;
  private String regionCode;
  private String unitCode;
  private String unitName;
  private String address;
  private LocalDate establishedDate;
  private String description;
  private String logoPath;

  @DynamoDbPartitionKey
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getRegionCode() {
    return regionCode;
  }

  public void setRegionCode(String regionCode) {
    this.regionCode = regionCode;
  }

  public String getUnitCode() {
    return unitCode;
  }

  public void setUnitCode(String unitCode) {
    this.unitCode = unitCode;
  }

  public String getUnitName() {
    return unitName;
  }

  public void setUnitName(String unitName) {
    this.unitName = unitName;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public LocalDate getEstablishedDate() {
    return establishedDate;
  }

  public void setEstablishedDate(LocalDate establishedDate) {
    this.establishedDate = establishedDate;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getLogoPath() {
    return logoPath;
  }

  public void setLogoPath(String logoPath) {
    this.logoPath = logoPath;
  }
}
