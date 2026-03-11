package com.military.controllers;

import com.military.payload.request.SubmissionGroupRequest;
import com.military.payload.request.SubmissionGroupUsersRequest;
import com.military.payload.response.BaseResponse;
import com.military.payload.response.SubmissionGroupResponse;
import com.military.service.SubmissionGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/submission-groups")
@Tag(name = "Submission Group", description = "API quan ly nhom trinh")
public class SubmissionGroupController {
  private final SubmissionGroupService submissionGroupService;

  public SubmissionGroupController(SubmissionGroupService submissionGroupService) {
    this.submissionGroupService = submissionGroupService;
  }

  @PostMapping
  @Operation(summary = "Them nhom trinh")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Tao thanh cong"),
      @ApiResponse(responseCode = "400", description = "Du lieu khong hop le",
          content = @Content(schema = @Schema(implementation = BaseResponse.class)))
  })
  public ResponseEntity<BaseResponse<SubmissionGroupResponse>> create(@Valid @RequestBody SubmissionGroupRequest request,
                                                                      HttpServletRequest httpRequest) {
    SubmissionGroupResponse response = submissionGroupService.create(request);
    return ResponseEntity.ok(BaseResponse.of(200, response, httpRequest.getServletPath()));
  }

  @PutMapping("/{id}")
  @Operation(summary = "Cap nhat nhom trinh")
  public ResponseEntity<BaseResponse<SubmissionGroupResponse>> update(@PathVariable Long id,
                                                                      @Valid @RequestBody SubmissionGroupRequest request,
                                                                      HttpServletRequest httpRequest) {
    SubmissionGroupResponse response = submissionGroupService.update(id, request);
    return ResponseEntity.ok(BaseResponse.of(200, response, httpRequest.getServletPath()));
  }

  @PostMapping("/{id}/users")
  @Operation(summary = "Them user vao nhom trinh")
  public ResponseEntity<BaseResponse<SubmissionGroupResponse>> addUsers(@PathVariable Long id,
                                                                        @Valid @RequestBody SubmissionGroupUsersRequest request,
                                                                        HttpServletRequest httpRequest) {
    SubmissionGroupResponse response = submissionGroupService.addUsers(id, request.getUserIds());
    return ResponseEntity.ok(BaseResponse.of(200, response, httpRequest.getServletPath()));
  }

  @DeleteMapping("/{id}/users")
  @Operation(summary = "Xoa user khoi nhom trinh")
  public ResponseEntity<BaseResponse<SubmissionGroupResponse>> removeUsers(@PathVariable Long id,
                                                                           @Valid @RequestBody SubmissionGroupUsersRequest request,
                                                                           HttpServletRequest httpRequest) {
    SubmissionGroupResponse response = submissionGroupService.removeUsers(id, request.getUserIds());
    return ResponseEntity.ok(BaseResponse.of(200, response, httpRequest.getServletPath()));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Xoa nhom trinh")
  public ResponseEntity<BaseResponse<String>> delete(@PathVariable Long id, HttpServletRequest request) {
    submissionGroupService.delete(id);
    return ResponseEntity.ok(BaseResponse.of(200, "Deleted successfully", request.getServletPath()));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Chi tiet nhom trinh")
  public ResponseEntity<BaseResponse<SubmissionGroupResponse>> detail(@PathVariable Long id, HttpServletRequest request) {
    SubmissionGroupResponse response = submissionGroupService.getById(id);
    return ResponseEntity.ok(BaseResponse.of(200, response, request.getServletPath()));
  }

  @GetMapping
  @Operation(summary = "Danh sach nhom trinh co phan trang")
  public ResponseEntity<BaseResponse<Page<SubmissionGroupResponse>>> list(
      @Parameter(description = "So trang, bat dau tu 0", example = "0")
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @Parameter(description = "So ban ghi moi trang", example = "10")
      @RequestParam(defaultValue = "10") @Min(1) int size,
      @Parameter(description = "Tu khoa tim kiem theo ten nhom trinh", example = "tieu doan")
      @RequestParam(required = false) String keyword,
      HttpServletRequest request) {
    PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
    Page<SubmissionGroupResponse> response = submissionGroupService.list(keyword, pageRequest);
    return ResponseEntity.ok(BaseResponse.of(200, response, request.getServletPath()));
  }
}
