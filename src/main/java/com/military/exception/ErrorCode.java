package com.military.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
  USERNAME_ALREADY_TAKEN("MIL00001", "error.username.already_taken", HttpStatus.BAD_REQUEST),
  EMAIL_ALREADY_IN_USE("MIL00002", "error.email.already_in_use", HttpStatus.BAD_REQUEST),
  ROLE_NOT_FOUND("MIL00003", "error.role.not_found", HttpStatus.NOT_FOUND),
  UNAUTHORIZED("MIL00004", "error.unauthorized", HttpStatus.UNAUTHORIZED),
  VALIDATION_FAILED("MIL00005", "error.validation.failed", HttpStatus.BAD_REQUEST),
  INTERNAL_SERVER_ERROR("MIL00006", "error.internal_server_error", HttpStatus.INTERNAL_SERVER_ERROR),
  USERNAME_PASSWORD_INCORRECT("MIL00007", "error.username_password_error", HttpStatus.BAD_REQUEST),
  PERSONNEL_NOT_FOUND("MIL00008", "error.personnel.not_found", HttpStatus.NOT_FOUND),
  PERSONNEL_QR_GENERATE_FAILED("MIL00009", "error.personnel.qr_generate_failed", HttpStatus.INTERNAL_SERVER_ERROR),
  PERSONNEL_IMAGE_SAVE_FAILED("MIL00010", "error.personnel.image_save_failed", HttpStatus.INTERNAL_SERVER_ERROR),
  PERSONNEL_IMAGE_DELETE_FAILED("MIL00011", "error.personnel.image_delete_failed", HttpStatus.INTERNAL_SERVER_ERROR),
  PERSONNEL_INVALID_INPUT("MIL00012", "error.personnel.invalid_input", HttpStatus.BAD_REQUEST),
  PERSONNEL_ALREADY_ASSIGNED("MIL00013", "error.personnel.already_assigned", HttpStatus.BAD_REQUEST),
  MILITARY_REGION_NOT_FOUND("MIL00014", "error.military_region.not_found", HttpStatus.NOT_FOUND),
  MILITARY_REGION_CODE_EXISTS("MIL00015", "error.military_region.code_exists", HttpStatus.BAD_REQUEST),
  MILITARY_REGION_LOGO_SAVE_FAILED("MIL00016", "error.military_region.logo_save_failed",
      HttpStatus.INTERNAL_SERVER_ERROR),
  MILITARY_REGION_LOGO_DELETE_FAILED("MIL00017", "error.military_region.logo_delete_failed",
      HttpStatus.INTERNAL_SERVER_ERROR),
  COMMON_INVALID_FILE_CATEGORY("MIL00018", "error.common.invalid_file_category", HttpStatus.BAD_REQUEST),
  MILITARY_UNIT_NOT_FOUND("MIL00019", "error.military_unit.not_found", HttpStatus.NOT_FOUND),
  MILITARY_UNIT_CODE_EXISTS("MIL00020", "error.military_unit.code_exists", HttpStatus.BAD_REQUEST),
  MILITARY_UNIT_LOGO_SAVE_FAILED("MIL00021", "error.military_unit.logo_save_failed",
      HttpStatus.INTERNAL_SERVER_ERROR),
  MILITARY_UNIT_LOGO_DELETE_FAILED("MIL00022", "error.military_unit.logo_delete_failed",
      HttpStatus.INTERNAL_SERVER_ERROR),
  SUBMISSION_FLOW_NOT_FOUND("MIL00023", "error.submission_flow.not_found", HttpStatus.NOT_FOUND),
  SUBMISSION_FLOW_INVALID_GROUPS("MIL00024", "error.submission_flow.invalid_groups", HttpStatus.BAD_REQUEST),
  SUBMISSION_FLOW_USER_NOT_FOUND("MIL00025", "error.submission_flow.user_not_found", HttpStatus.BAD_REQUEST),
  SUBMISSION_GROUP_NOT_FOUND("MIL00026", "error.submission_group.not_found", HttpStatus.NOT_FOUND),
  SUBMISSION_GROUP_IN_USE("MIL00027", "error.submission_group.in_use", HttpStatus.BAD_REQUEST),
  SUBMISSION_GROUP_INVALID_USERS("MIL00028", "error.submission_group.invalid_users", HttpStatus.BAD_REQUEST),
  LEAVE_APPROVAL_CONFIG_NOT_FOUND("MIL00029", "error.leave_approval_config.not_found", HttpStatus.NOT_FOUND),
  LEAVE_APPROVAL_CONFIG_INVALID_EFFECTIVE_RANGE("MIL00030", "error.leave_approval_config.invalid_effective_range",
      HttpStatus.BAD_REQUEST),
  LEAVE_APPROVAL_CONFIG_INVALID_ACTIVE("MIL00031", "error.leave_approval_config.invalid_active", HttpStatus.BAD_REQUEST),
  LEAVE_APPROVAL_CONFIG_INVALID_APPLICABLE_QUERY("MIL00032", "error.leave_approval_config.invalid_applicable_query",
      HttpStatus.BAD_REQUEST),
  LEAVE_APPROVAL_CONFIG_DUPLICATE_UNIQUE_FIELDS("MIL00033", "error.leave_approval_config.duplicate_unique_fields",
      HttpStatus.BAD_REQUEST),
  LEAVE_APPROVAL_CONFIG_OVERLAPPING_RANGE("MIL00034", "error.leave_approval_config.overlapping_range",
      HttpStatus.BAD_REQUEST),
  LEAVE_REQUEST_NOT_FOUND("MIL00035", "error.leave_request.not_found", HttpStatus.NOT_FOUND),
  LEAVE_REQUEST_INVALID_RANGE("MIL00036", "error.leave_request.invalid_range", HttpStatus.BAD_REQUEST),
  LEAVE_REQUEST_DEFAULT_FLOW_NOT_FOUND("MIL00037", "error.leave_request.default_flow_not_found", HttpStatus.NOT_FOUND),
  LEAVE_REQUEST_REQUESTER_NOT_IN_FLOW("MIL00038", "error.leave_request.requester_not_in_flow", HttpStatus.BAD_REQUEST),
  LEAVE_REQUEST_NEXT_ASSIGNEE_NOT_FOUND("MIL00039", "error.leave_request.next_assignee_not_found", HttpStatus.BAD_REQUEST),
  LEAVE_REQUEST_HISTORY_NOT_FOUND("MIL00040", "error.leave_request.history_not_found", HttpStatus.NOT_FOUND),
  LEAVE_REQUEST_INVALID_STATE("MIL00041", "error.leave_request.invalid_state", HttpStatus.BAD_REQUEST),
  LEAVE_REQUEST_FORBIDDEN("MIL00042", "error.leave_request.forbidden", HttpStatus.FORBIDDEN),
  LEAVE_REQUEST_APPROVAL_LIMIT_EXCEEDED("MIL00043", "error.leave_request.approval_limit_exceeded", HttpStatus.BAD_REQUEST),
  SUBMISSION_FLOW_CODE_EXISTS("MIL00044", "error.submission_flow.code_exists", HttpStatus.BAD_REQUEST),
  NO_APPROVE_AUTHORITY("MIL00045", "error.leave_request.no_approve_authority", HttpStatus.FORBIDDEN);

  private final String code;
  private final String messageKey;
  private final HttpStatus httpStatus;

  ErrorCode(String code, String messageKey, HttpStatus httpStatus) {
    this.code = code;
    this.messageKey = messageKey;
    this.httpStatus = httpStatus;
  }

  public String getCode() {
    return code;
  }

  public String getMessageKey() {
    return messageKey;
  }

  public HttpStatus getHttpStatus() {
    return httpStatus;
  }
}
