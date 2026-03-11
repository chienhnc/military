package com.military.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "Thong tin quan khu")
public class MilitaryRegionRequest {

  @Schema(description = "Ten quan khu", example = "Quan khu 7")
  @NotBlank
  @Size(max = 200)
  private String regionName;

  @Schema(description = "Ma quan khu", example = "QK7")
  @NotBlank
  @Size(max = 50)
  private String regionCode;

  @Schema(description = "Ngay thanh lap", example = "1945-09-02")
  @NotNull
  private LocalDate establishedDate;

  @Schema(description = "Mo ta", example = "Quan khu phu trach khu vuc Dong Nam Bo")
  @Size(max = 1000)
  private String description;

  @Schema(description = "Ten file logo da upload qua API /api/common/upload-image?category=region", example = "9ca934b5-5a58-42d9-ad02-1a4a31f7617f.png")
  @Size(max = 255)
  private String logoPath;
}
