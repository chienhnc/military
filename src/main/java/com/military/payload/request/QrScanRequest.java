package com.military.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;

@Schema(description = "Payload scan QR, chi duoc truyen 1 trong 2 object: militaryPersonnel hoac citizen")
public class QrScanRequest {
  @Valid
  private QrMilitaryPersonnelDataRequest militaryPersonnel;

  @Valid
  private QrCitizenDataRequest citizen;

  public QrMilitaryPersonnelDataRequest getMilitaryPersonnel() {
    return militaryPersonnel;
  }

  public void setMilitaryPersonnel(QrMilitaryPersonnelDataRequest militaryPersonnel) {
    this.militaryPersonnel = militaryPersonnel;
  }

  public QrCitizenDataRequest getCitizen() {
    return citizen;
  }

  public void setCitizen(QrCitizenDataRequest citizen) {
    this.citizen = citizen;
  }
}
