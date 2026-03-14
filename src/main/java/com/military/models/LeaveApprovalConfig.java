package com.military.models;

import com.military.payload.request.LeaveApprovalConfigRequest;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class LeaveApprovalConfig {
  private Long id;
  private EMilitaryPosition militaryPosition;
  private Integer maxApprovalDays;
  private LocalDate effectiveFrom;
  private LocalDate effectiveTo;
  private Boolean active;

  public LeaveApprovalConfig(LeaveApprovalConfigRequest request) {
    BeanUtils.copyProperties(request, this);
  }
}
