package com.military.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Schema(description = "Thong tin tao yeu cau nghi phep")
public class LeaveRequestCreateRequest {
  @NotNull
  @Schema(description = "Ngay bat dau nghi", example = "2026-03-20")
  private LocalDate leaveFrom;

  @NotNull
  @Schema(description = "Ngay ket thuc nghi", example = "2026-03-22")
  private LocalDate leaveTo;

  @Schema(description = "Ly do yeu cau", example = "Nghi phep nam")
  private String reason;

  @NotNull
  @Min(0)
  @Schema(description = "So lan duoc phep ra", example = "3")
  private Integer allowedOutCount;

  public LocalDate getLeaveFrom() {
    return leaveFrom;
  }

  public void setLeaveFrom(LocalDate leaveFrom) {
    this.leaveFrom = leaveFrom;
  }

  public LocalDate getLeaveTo() {
    return leaveTo;
  }

  public void setLeaveTo(LocalDate leaveTo) {
    this.leaveTo = leaveTo;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public Integer getAllowedOutCount() {
    return allowedOutCount;
  }

  public void setAllowedOutCount(Integer allowedOutCount) {
    this.allowedOutCount = allowedOutCount;
  }
}
