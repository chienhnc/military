package com.military.payload.response;

import com.military.models.MilitaryRegion;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.time.LocalDate;

@Data
public class MilitaryRegionResponse {
  private Long id;
  private String regionName;
  private String regionCode;
  private LocalDate establishedDate;
  private String description;
  private String logoUrl;

  public MilitaryRegionResponse(MilitaryRegion militaryRegion) {
    BeanUtils.copyProperties(militaryRegion, this);
  }
}
