package com.military.repository.dynamodb.item;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.time.LocalDate;

@DynamoDbBean
public class MilitaryRegionItem {
  private Long id;
  private String regionName;
  private String regionCode;
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

  public String getRegionName() {
    return regionName;
  }

  public void setRegionName(String regionName) {
    this.regionName = regionName;
  }

  public String getRegionCode() {
    return regionCode;
  }

  public void setRegionCode(String regionCode) {
    this.regionCode = regionCode;
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
