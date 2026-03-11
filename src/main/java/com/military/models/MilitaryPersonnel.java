package com.military.models;

import com.military.payload.request.MilitaryPersonnelRequest;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

@Data
@NoArgsConstructor
public class MilitaryPersonnel {
  private Long id;
  private String code;
  private String fullName;
  private String regionCode;
  private EMilitaryRank rankCode;
  private String unitCode;
  private EMilitaryPosition positionCode;
  private String qrCode;
  private String imagePath;

  public MilitaryPersonnel(MilitaryPersonnelRequest militaryPersonnelRequest) {
    BeanUtils.copyProperties(militaryPersonnelRequest, this);
  }
}
