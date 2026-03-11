package com.military.controllers;

import com.military.payload.request.LoginRequest;
import com.military.payload.response.BaseResponse;
import com.military.payload.request.SignupRequest;
import com.military.payload.response.UserInfoResponse;
import com.military.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

//for Angular Client (withCredentials)
//@CrossOrigin(origins = "http://localhost:8081", maxAge = 3600, allowCredentials="true")
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "API dang nhap, dang ky va quan ly tai khoan")
public class AuthController {
  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/signin")
  @Operation(
      summary = "Dang nhap",
      description = "Xac thuc username/password va tra ve JWT token."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Dang nhap thanh cong"),
      @ApiResponse(responseCode = "400", description = "Sai username/password",
          content = @Content(schema = @Schema(implementation = BaseResponse.class)))
  })
  public ResponseEntity<BaseResponse<UserInfoResponse>> authenticateUser(@Valid @RequestBody LoginRequest loginRequest,
         HttpServletRequest request) {
    UserInfoResponse responseData = authService.authenticateUser(loginRequest);
    return ResponseEntity.ok(BaseResponse.of(200, responseData, request.getServletPath()));
  }

  @PostMapping("/signup")
  @Operation(
      summary = "Dang ky user + tao quan nhan",
      description = "Tao user moi va dong thoi tao thong tin quan nhan, gan quan he 1-1 user <-> quan nhan."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Dang ky thanh cong"),
      @ApiResponse(responseCode = "400", description = "Du lieu khong hop le hoac username/email trung",
          content = @Content(schema = @Schema(implementation = BaseResponse.class)))
  })
  public ResponseEntity<BaseResponse<String>> registerUser(@Valid @RequestBody SignupRequest signUpRequest,
                                                           HttpServletRequest request) {
    String message = authService.registerUser(signUpRequest);
    return ResponseEntity.ok(BaseResponse.of(200, message, request.getServletPath()));
  }

  @PostMapping("/signout")
  @Operation(
      summary = "Dang xuat",
      description = "He thong JWT stateless khong can huy session server-side."
  )
  @ApiResponse(responseCode = "200", description = "Xu ly dang xuat thanh cong")
  public ResponseEntity<BaseResponse<String>> logoutUser(HttpServletRequest request) {
    String message = authService.logoutUser();
    return ResponseEntity.ok(BaseResponse.of(200, message, request.getServletPath()));
  }
}
