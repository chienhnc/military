package com.military.payload.request;

import com.military.models.EMilitaryPosition;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "Thong tin cau hinh phe duyet ngay nghi phep")
public class LeaveApprovalConfigRequest {
  @Schema(description = "Chuc vu co quyen duyet", example = "CHI_HUY_TRUONG")
  @NotNull
  private EMilitaryPosition militaryPosition;

  @Schema(description = "So ngay nghi phep toi da duoc quyen duyet", example = "7")
  @NotNull
  @Min(1)
  private Integer maxApprovalDays;

  @Schema(description = "Ngay bat dau ap dung", example = "2026-01-01")
  @NotNull
  private LocalDate effectiveFrom;

  @Schema(description = "Ngay ket thuc ap dung", example = "2026-12-31")
  @NotNull
  private LocalDate effectiveTo;

  @Schema(description = "Co ap dung cau hinh hay khong", example = "true")
  @NotNull
  private Boolean active;
}
