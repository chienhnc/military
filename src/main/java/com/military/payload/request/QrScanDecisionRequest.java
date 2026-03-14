package com.military.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Yeu cau dong y/tu choi doi voi log scan nguoi dan")
public class QrScanDecisionRequest {
  private String reason;

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }
}
