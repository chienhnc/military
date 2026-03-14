package com.military.payload.response;

import com.military.models.LeaveRequest;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.time.LocalDate;

@Data
public class LeaveRequestResponse {
  private Long id;
  private Long militaryPersonnelId;
  private Long userId;
  private String createdAt;
  private LocalDate leaveFrom;
  private LocalDate leaveTo;
  private String status;
  private Long flowId;
  private Integer currentOrderNo;
  private String currentRound;
  private String currentAssignee;
  private String reason;

  public LeaveRequestResponse(LeaveRequest leaveRequest) {
    BeanUtils.copyProperties(leaveRequest, this);
    this.status = leaveRequest.getStatus() == null ? null : leaveRequest.getStatus().name();
  }
}
