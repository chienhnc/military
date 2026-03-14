package com.military.payload.response;

import com.military.models.LeaveRequestHistory;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.time.LocalDate;

@Data
public class LeaveRequestHistoryResponse {
  private Long id;
  private Long leaveRequestId;
  private String roundNo;
  private Long militaryPersonnelId;
  private Long userId;
  private String createdAt;
  private LocalDate leaveFrom;
  private LocalDate leaveTo;
  private String status;
  private String assignee;
  private Long flowId;
  private Integer orderNo;
  private String reason;

  public LeaveRequestHistoryResponse(LeaveRequestHistory history) {
    BeanUtils.copyProperties(history, this);
    this.status = history.getStatus() == null ? null : history.getStatus().name();
  }
}
