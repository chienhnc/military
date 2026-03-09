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
  PERSONNEL_ALREADY_ASSIGNED("MIL00013", "error.personnel.already_assigned", HttpStatus.BAD_REQUEST);

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
