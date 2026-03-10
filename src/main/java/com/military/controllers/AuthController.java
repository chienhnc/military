package com.military.controllers;

import com.military.models.ERole;
import com.military.models.MilitaryPersonnel;
import com.military.models.Role;
import com.military.models.User;
import com.military.exception.AppException;
import com.military.exception.ErrorCode;
import com.military.payload.request.LoginRequest;
import com.military.payload.response.MilitaryPersonnelResponse;
import com.military.payload.response.BaseResponse;
import com.military.payload.request.SignupRequest;
import com.military.payload.response.UserInfoResponse;
import com.military.repository.RoleRepository;
import com.military.repository.UserRepository;
import com.military.repository.MilitaryPersonnelRepository;
import com.military.service.MilitaryPersonnelService;
import com.military.security.jwt.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import com.military.security.services.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

//for Angular Client (withCredentials)
//@CrossOrigin(origins = "http://localhost:8081", maxAge = 3600, allowCredentials="true")
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "API dang nhap, dang ky va quan ly tai khoan")
public class AuthController {
  @Autowired
  AuthenticationManager authenticationManager;

  @Autowired
  UserRepository userRepository;

  @Autowired
  RoleRepository roleRepository;

  @Autowired
  PasswordEncoder encoder;

  @Autowired
  JwtUtils jwtUtils;

  @Autowired
  MilitaryPersonnelRepository militaryPersonnelRepository;

  @Autowired
  MilitaryPersonnelService militaryPersonnelService;

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
    try {
      Authentication authentication = authenticationManager.authenticate(
              new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

      SecurityContextHolder.getContext().setAuthentication(authentication);

      UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
      String jwt = jwtUtils.generateTokenFromUsername(userDetails.getUsername());

      List<String> roles = userDetails.getAuthorities().stream()
              .map(GrantedAuthority::getAuthority)
              .collect(Collectors.toList());

      UserInfoResponse responseData = new UserInfoResponse(jwt, userDetails.getId(), userDetails.getUsername(),
              userDetails.getEmail(), roles);

      return ResponseEntity.ok(BaseResponse.of(200, responseData, request.getServletPath()));
    } catch (Exception e) {
      throw new AppException(ErrorCode.USERNAME_PASSWORD_INCORRECT);
    }
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
    if (userRepository.existsByUsername(signUpRequest.getUsername())) {
      throw new AppException(ErrorCode.USERNAME_ALREADY_TAKEN);
    }

    if (userRepository.existsByEmail(signUpRequest.getEmail())) {
      throw new AppException(ErrorCode.EMAIL_ALREADY_IN_USE);
    }

    MilitaryPersonnelResponse createdPersonnel = militaryPersonnelService.create(signUpRequest.getMilitaryPersonnel());
    MilitaryPersonnel militaryPersonnel = militaryPersonnelRepository.findById(createdPersonnel.getId())
        .orElseThrow(() -> new AppException(ErrorCode.PERSONNEL_NOT_FOUND));

    // Create new user's account
    User user = new User(signUpRequest.getUsername(),
                         signUpRequest.getEmail(),
                         encoder.encode(signUpRequest.getPassword()));

    Set<String> strRoles = signUpRequest.getRole();
    Set<Role> roles = new HashSet<>();

    if (strRoles == null) {
      Role userRole = roleRepository.findByName(ERole.ROLE_USER)
          .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
      roles.add(userRole);
    } else {
      strRoles.forEach(role -> {
        switch (role) {
        case "admin":
          Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
              .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
          roles.add(adminRole);

          break;
        case "mod":
          Role modRole = roleRepository.findByName(ERole.ROLE_MODERATOR)
              .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
          roles.add(modRole);

          break;
        default:
          Role userRole = roleRepository.findByName(ERole.ROLE_USER)
              .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
          roles.add(userRole);
        }
      });
    }

    user.setRoles(roles);
    user.setMilitaryPersonnel(militaryPersonnel);
    userRepository.save(user);

    return ResponseEntity.ok(BaseResponse.of(200, "User registered successfully!", request.getServletPath()));
  }

  @PostMapping("/signout")
  @Operation(
      summary = "Dang xuat",
      description = "He thong JWT stateless khong can huy session server-side."
  )
  @ApiResponse(responseCode = "200", description = "Xu ly dang xuat thanh cong")
  public ResponseEntity<BaseResponse<String>> logoutUser(HttpServletRequest request) {
    return ResponseEntity.ok(
        BaseResponse.of(200, "Stateless JWT does not require server-side signout.", request.getServletPath()));
  }
}
