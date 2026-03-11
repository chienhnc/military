package com.military.models;

import com.military.payload.request.MilitaryUnitRequest;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class MilitaryUnit {
  private Long id;
  private String regionCode;
  private String unitCode;
  private String unitName;
  private String address;
  private LocalDate establishedDate;
  private String description;
  private String logoPath;

  public MilitaryUnit(MilitaryUnitRequest request) {
    BeanUtils.copyProperties(request, this);
  }
}
