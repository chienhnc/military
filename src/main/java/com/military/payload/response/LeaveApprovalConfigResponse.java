package com.military.payload.response;

import com.military.models.LeaveApprovalConfig;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.time.LocalDate;

@Data
public class LeaveApprovalConfigResponse {
  private Long id;
  private String militaryPosition;
  private String militaryPositionName;
  private Integer maxApprovalDays;
  private LocalDate effectiveFrom;
  private LocalDate effectiveTo;
  private Boolean active;

  public LeaveApprovalConfigResponse(LeaveApprovalConfig config) {
    BeanUtils.copyProperties(config, this);
    if (config.getMilitaryPosition() != null) {
      this.militaryPosition = config.getMilitaryPosition().getCode();
      this.militaryPositionName = config.getMilitaryPosition().getName();
    }
  }
}
