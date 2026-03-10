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
  private String rankCode;
  private String unitCode;
  private String positionCode;
  private String qrCode;
  private String imagePath;

  public MilitaryPersonnel(MilitaryPersonnelRequest militaryPersonnelRequest) {
    BeanUtils.copyProperties(militaryPersonnelRequest, this);
  }
}
