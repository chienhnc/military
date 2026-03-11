package com.military.models;

public enum EMilitaryPosition {
  BO_TRUONG_BO_QUOC_PHONG("Bộ trưởng Bộ Quốc phòng"),
  TONG_THAM_MUU_TRUONG("Tổng Tham mưu trưởng"),
  CHU_NHIEM_TONG_CUC("Chủ nhiệm Tổng cục"),
  TU_LENH_CHINH_UY("Tư lệnh/Chính ủy"),
  SU_DOAN_TRUONG("Sư đoàn trưởng"),
  CHI_HUY_TRUONG("Chỉ huy trưởng"),
  LU_DOAN_TRUONG("Lữ đoàn trưởng"),
  TRUNG_DOAN_TRUONG("Trung đoàn trưởng"),
  TIEU_DOAN_TRUONG("Tiểu đoàn trưởng"),
  DAI_DOI_TRUONG("Đại đội trưởng"),
  TRUNG_DOI_TRUONG("Trung đội trưởng"),
  TIEU_DOI_TRUONG("Tiểu đội trưởng"),
  CHIEN_SY("Chiến sỹ");

  private final String name;

  EMilitaryPosition(String name) {
    this.name = name;
  }

  public String getCode() {
    return name();
  }

  public String getName() {
    return name;
  }
}
