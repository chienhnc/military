package com.military.models;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class LeaveRequest {
  private Long id;
  private Long militaryPersonnelId;
  private Long userId;
  private String createdAt;
  private LocalDate leaveFrom;
  private LocalDate leaveTo;
  private ELeaveRequestStatus status;
  private Long flowId;
  private Integer currentOrderNo;
  private String currentRound;
  private String currentAssignee;
  private String reason;
}
