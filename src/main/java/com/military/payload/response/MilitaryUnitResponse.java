package com.military.payload.response;

import com.military.models.MilitaryUnit;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.time.LocalDate;

@Data
public class MilitaryUnitResponse {
  private Long id;
  private String regionCode;
  private String unitCode;
  private String unitName;
  private String address;
  private LocalDate establishedDate;
  private String description;
  private String logoUrl;

  public MilitaryUnitResponse(MilitaryUnit unit) {
    BeanUtils.copyProperties(unit, this);
  }
}
