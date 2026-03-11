package com.military.models;

public enum EMilitaryRank {
  DAI_TUONG("Đại tướng"),
  THUONG_TUONG("Thượng tướng"),
  TRUNG_TUONG("Trung tướng"),
  THIEU_TUONG("Thiếu tướng"),
  DAI_TA("Đại tá"),
  THUONG_TA("Thượng tá"),
  TRUNG_TA("Trung tá"),
  THIEU_TA("Thiếu tá"),
  DAI_UY("Đại úy"),
  THUONG_UY("Thượng úy"),
  TRUNG_UY("Trung úy"),
  THIEU_UY("Thiếu úy"),
  THUONG_SI("Thượng sĩ"),
  TRUNG_SI("Trung sĩ"),
  HA_SI("Hạ sĩ"),
  BINH_NHAT("Binh nhất"),
  BINH_NHI("Binh nhì");

  private final String name;

  EMilitaryRank(String name) {
    this.name = name;
  }

  public String getCode() {
    return name();
  }

  public String getName() {
    return name;
  }
}
