package com.military.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Thong tin luong trinh")
public class SubmissionFlowRequest {
  @Schema(description = "Ten luong trinh", example = "Luong trinh phe duyet cap don vi")
  @NotBlank
  @Size(max = 255)
  private String name;

  @Schema(description = "Mo ta", example = "Luong trinh gom nhieu nhom trinh theo thu tu")
  @Size(max = 1000)
  private String description;

  @Schema(description = "Danh sach nhom trinh")
  @NotEmpty
  @Valid
  private List<SubmissionFlowGroupRequest> groups;
}
