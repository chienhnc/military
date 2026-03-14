package com.military.payload.response;

import com.military.models.QrScanLog;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.time.LocalDate;

@Data
public class QrScanLogResponse {
  private Long id;
  private String scanType;
  private String scannedAt;
  private String status;
  private String reason;

  private Long militaryPersonnelId;
  private String militaryPersonnelCode;
  private String militaryPersonnelFullName;

  private String citizenName;
  private LocalDate citizenBirthday;
  private String citizenAddress;
  private String citizenId;
  private LocalDate citizenIssueDate;

  private Long leaveRequestId;
  private String approvedRoundNo;

  public QrScanLogResponse(QrScanLog model) {
    BeanUtils.copyProperties(model, this);
    this.scanType = model.getScanType() == null ? null : model.getScanType().name();
    this.status = model.getStatus() == null ? null : model.getStatus().name();
  }
}
