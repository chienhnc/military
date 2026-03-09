package com.military.payload.response;

import com.military.models.MilitaryPersonnel;
import lombok.Data;
import org.springframework.beans.BeanUtils;

@Data
public class MilitaryPersonnelResponse {
  private Long id;
  private String fullName;
  private String rank;
  private String unitName;
  private String position;
  private String code;
  private String qrCode;
  private String imageUrl;

  public MilitaryPersonnelResponse(MilitaryPersonnel militaryPersonnel) {
    BeanUtils.copyProperties(militaryPersonnel, this);
  }
}
