package com.military.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Du lieu QR cua quan nhan")
public class QrMilitaryPersonnelDataRequest {
  private Long id;
  private String code;
  private String fullName;
  private String regionCode;
  private String rankCode;
  private String unitCode;
  private String positionCode;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public String getRegionCode() {
    return regionCode;
  }

  public void setRegionCode(String regionCode) {
    this.regionCode = regionCode;
  }

  public String getRankCode() {
    return rankCode;
  }

  public void setRankCode(String rankCode) {
    this.rankCode = rankCode;
  }

  public String getUnitCode() {
    return unitCode;
  }

  public void setUnitCode(String unitCode) {
    this.unitCode = unitCode;
  }

  public String getPositionCode() {
    return positionCode;
  }

  public void setPositionCode(String positionCode) {
    this.positionCode = positionCode;
  }
}
