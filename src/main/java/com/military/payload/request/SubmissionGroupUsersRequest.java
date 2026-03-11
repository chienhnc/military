package com.military.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Danh sach user can thao tac trong nhom trinh")
public class SubmissionGroupUsersRequest {
  @Schema(description = "Danh sach user id", example = "[1001, 1002]")
  @NotEmpty
  private List<Long> userIds;
}
