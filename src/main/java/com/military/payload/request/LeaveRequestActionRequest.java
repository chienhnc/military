package com.military.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Thong tin thao tac duyet/tra ve/trinh tiep")
public class LeaveRequestActionRequest {
  @Schema(description = "Ly do", example = "Dong y theo de nghi")
  private String reason;

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }
}
