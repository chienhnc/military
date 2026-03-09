package com.military.controllers;

import com.military.payload.response.BaseResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

//for Angular Client (withCredentials)
//@CrossOrigin(origins = "http://localhost:8081", maxAge = 3600, allowCredentials="true")
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/test")
public class TestController {
  @GetMapping("/all")
  public BaseResponse<String> allAccess(HttpServletRequest request) {
    return BaseResponse.of(200, "Public Content.", request.getServletPath());
  }

  @GetMapping("/user")
  @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
  public BaseResponse<String> userAccess(HttpServletRequest request) {
    return BaseResponse.of(200, "User Content.", request.getServletPath());
  }

  @GetMapping("/mod")
  @PreAuthorize("hasRole('MODERATOR')")
  public BaseResponse<String> moderatorAccess(HttpServletRequest request) {
    return BaseResponse.of(200, "Moderator Board.", request.getServletPath());
  }

  @GetMapping("/admin")
  @PreAuthorize("hasRole('ADMIN')")
  public BaseResponse<String> adminAccess(HttpServletRequest request) {
    return BaseResponse.of(200, "Admin Board.", request.getServletPath());
  }
}
