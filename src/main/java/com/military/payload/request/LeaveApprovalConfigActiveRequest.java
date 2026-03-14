package com.military.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Trang thai ap dung cau hinh nghi phep")
public class LeaveApprovalConfigActiveRequest {
  @Schema(description = "Co ap dung cau hinh hay khong", example = "false")
  @NotNull
  private Boolean active;
}
