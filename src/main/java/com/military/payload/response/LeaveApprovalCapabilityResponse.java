package com.military.payload.response;

import lombok.Data;

@Data
public class LeaveApprovalCapabilityResponse {
  private boolean canApprove;
  private Long leaveRequestId;
  private String militaryPosition;
  private Integer maxApprovalDays;
  private Integer requestedDays;
  private String reason;
}
