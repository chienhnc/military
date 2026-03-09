package com.military.exception;

import com.military.payload.response.BaseResponse;
import com.military.payload.response.ErrorDetail;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Locale;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private final MessageSource messageSource;

  public GlobalExceptionHandler(MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  @ExceptionHandler(AppException.class)
  public ResponseEntity<BaseResponse<ErrorDetail>> handleAppException(AppException ex, HttpServletRequest request) {
    return buildErrorResponse(ex.getErrorCode(), request.getServletPath());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<BaseResponse<ErrorDetail>> handleValidationException(MethodArgumentNotValidException ex,
                                                                             HttpServletRequest request) {
    ErrorCode errorCode = ErrorCode.VALIDATION_FAILED;
    HttpStatus status = errorCode.getHttpStatus();
    String description = resolveMessage(errorCode);

    FieldError firstError = ex.getBindingResult().getFieldError();
    if (firstError != null) {
      description = firstError.getDefaultMessage();
    }

    ErrorDetail errorDetail = new ErrorDetail(errorCode.getCode(), description);
    BaseResponse<ErrorDetail> response = BaseResponse.of(status.value(), errorDetail, request.getServletPath());
    return ResponseEntity.status(status).body(response);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<BaseResponse<ErrorDetail>> handleGenericException(Exception ex, HttpServletRequest request) {
    return buildErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR, request.getServletPath());
  }

  private ResponseEntity<BaseResponse<ErrorDetail>> buildErrorResponse(ErrorCode errorCode, String path) {
    HttpStatus status = errorCode.getHttpStatus();
    ErrorDetail errorDetail = new ErrorDetail(errorCode.getCode(), resolveMessage(errorCode));
    BaseResponse<ErrorDetail> response = BaseResponse.of(status.value(), errorDetail, path);
    return ResponseEntity.status(status).body(response);
  }

  private String resolveMessage(ErrorCode errorCode) {
    Locale locale = LocaleContextHolder.getLocale();
    return messageSource.getMessage(errorCode.getMessageKey(), null, locale);
  }
}
