package com.military.models;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class QrScanLog {
  private Long id;
  private EQrScanType scanType;
  private String scannedAt;
  private EQrScanStatus status;
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
  private String payloadJson;
}
