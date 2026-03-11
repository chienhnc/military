package com.military.payload.response;

import com.military.models.EMilitaryPosition;
import com.military.models.EMilitaryRank;
import com.military.models.MilitaryPersonnel;
import lombok.Data;
import org.springframework.beans.BeanUtils;

@Data
public class MilitaryPersonnelResponse {
  private Long id;
  private String fullName;
  private String regionCode;
  private EMilitaryRank rankCode;
  private String unitCode;
  private EMilitaryPosition positionCode;
  private String code;
  private String qrCode;
  private String imageUrl;

  public MilitaryPersonnelResponse(MilitaryPersonnel militaryPersonnel) {
    BeanUtils.copyProperties(militaryPersonnel, this);
  }
}
