package com.military.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Nhom trinh trong luong trinh")
public class SubmissionFlowGroupRequest {
  @Schema(description = "Thu tu nhom trinh, bat dau tu 1", example = "1")
  @NotNull
  @Min(1)
  private Integer orderNo;

  @Schema(description = "ID nhom trinh duoc gan vao luong trinh", example = "1001")
  @NotNull
  private Long groupId;
}
