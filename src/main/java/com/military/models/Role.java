package com.military.models;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Role {
  private Integer id;
  private ERole name;

  public Role(ERole name) {
    this.name = name;
  }
}
