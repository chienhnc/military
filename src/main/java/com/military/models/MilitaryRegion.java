package com.military.models;

import com.military.payload.request.MilitaryRegionRequest;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class MilitaryRegion {
  private Long id;
  private String regionName;
  private String regionCode;
  private LocalDate establishedDate;
  private String description;
  private String logoPath;

  public MilitaryRegion(MilitaryRegionRequest request) {
    BeanUtils.copyProperties(request, this);
  }
}
