package com.military.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "Thong tin don vi")
public class MilitaryUnitRequest {

  @Schema(description = "Ma quan khu", example = "QK7")
  @NotBlank
  @Size(max = 50)
  private String regionCode;

  @Schema(description = "Ma don vi", example = "DV001")
  @NotBlank
  @Size(max = 50)
  private String unitCode;

  @Schema(description = "Ten don vi", example = "Trung doan 1")
  @NotBlank
  @Size(max = 200)
  private String unitName;

  @Schema(description = "Dia chi don vi", example = "So 1 Duong ABC, TP.HCM")
  @Size(max = 500)
  private String address;

  @Schema(description = "Ngay thanh lap", example = "1975-04-30")
  @NotNull
  private LocalDate establishedDate;

  @Schema(description = "Mo ta", example = "Don vi chu luc khu vuc phia Nam")
  @Size(max = 1000)
  private String description;

  @Schema(description = "Ten file logo da upload qua API /api/common/upload-image?category=unit", example = "2f19f7ff-c8c2-4207-997d-31f97ebf90fc.png")
  @Size(max = 255)
  private String logoPath;
}
