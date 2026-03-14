package com.military.models;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class LeaveRequestHistory {
  private Long id;
  private Long leaveRequestId;
  private String roundNo;
  private Long militaryPersonnelId;
  private Long userId;
  private String createdAt;
  private LocalDate leaveFrom;
  private LocalDate leaveTo;
  private ELeaveRequestStatus status;
  private String assignee;
  private Long flowId;
  private Integer orderNo;
  private String reason;
}
