package com.military.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Thong tin nhom trinh")
public class SubmissionGroupRequest {
  @Schema(description = "Ten nhom trinh", example = "Nhom trinh cap tieu doan")
  @Size(max = 255)
  private String name;

  @Schema(description = "Mo ta", example = "Nhom gom cac user cap tieu doan")
  @Size(max = 1000)
  private String description;
}
