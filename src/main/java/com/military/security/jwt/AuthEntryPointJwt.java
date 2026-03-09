package com.military.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.military.exception.ErrorCode;
import com.military.payload.response.BaseResponse;
import com.military.payload.response.ErrorDetail;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Locale;

@Component
public class AuthEntryPointJwt implements AuthenticationEntryPoint {

  private static final Logger logger = LoggerFactory.getLogger(AuthEntryPointJwt.class);
  private final MessageSource messageSource;

  public AuthEntryPointJwt(MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  @Override
  public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
      throws IOException, ServletException {
    logger.error("Unauthorized error: {}", authException.getMessage());

    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

    ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
    Locale locale = LocaleContextHolder.getLocale();
    String description = messageSource.getMessage(errorCode.getMessageKey(), null, locale);
    ErrorDetail errorDetail = new ErrorDetail(errorCode.getCode(), description);
    BaseResponse<ErrorDetail> body = BaseResponse.of(HttpServletResponse.SC_UNAUTHORIZED, errorDetail,
        request.getServletPath());

    final ObjectMapper mapper = new ObjectMapper();
    mapper.writeValue(response.getOutputStream(), body);
  }

}
