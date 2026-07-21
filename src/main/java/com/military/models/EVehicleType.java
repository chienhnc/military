package com.military.models;

public enum EVehicleType {
  CAR("Ô tô"),
  MOTORBIKE("Xe máy"),
  OTHER("Khác");

  private final String name;

  EVehicleType(String name) {
    this.name = name;
  }

  public String getCode() {
    return name();
  }

  public String getName() {
    return name;
  }
}
