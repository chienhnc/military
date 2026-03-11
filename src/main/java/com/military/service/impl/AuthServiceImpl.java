package com.military.service.impl;

import com.military.exception.AppException;
import com.military.exception.ErrorCode;
import com.military.models.ERole;
import com.military.models.MilitaryPersonnel;
import com.military.models.Role;
import com.military.models.User;
import com.military.payload.request.LoginRequest;
import com.military.payload.request.SignupRequest;
import com.military.payload.response.MilitaryPersonnelResponse;
import com.military.payload.response.UserInfoResponse;
import com.military.repository.MilitaryPersonnelRepository;
import com.military.repository.RoleRepository;
import com.military.repository.UserRepository;
import com.military.security.jwt.JwtUtils;
import com.military.security.services.UserDetailsImpl;
import com.military.service.AuthService;
import com.military.service.MilitaryPersonnelService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {
  private final AuthenticationManager authenticationManager;
  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder encoder;
  private final JwtUtils jwtUtils;
  private final MilitaryPersonnelRepository militaryPersonnelRepository;
  private final MilitaryPersonnelService militaryPersonnelService;

  public AuthServiceImpl(AuthenticationManager authenticationManager,
                         UserRepository userRepository,
                         RoleRepository roleRepository,
                         PasswordEncoder encoder,
                         JwtUtils jwtUtils,
                         MilitaryPersonnelRepository militaryPersonnelRepository,
                         MilitaryPersonnelService militaryPersonnelService) {
    this.authenticationManager = authenticationManager;
    this.userRepository = userRepository;
    this.roleRepository = roleRepository;
    this.encoder = encoder;
    this.jwtUtils = jwtUtils;
    this.militaryPersonnelRepository = militaryPersonnelRepository;
    this.militaryPersonnelService = militaryPersonnelService;
  }

  @Override
  public UserInfoResponse authenticateUser(LoginRequest loginRequest) {
    try {
      Authentication authentication = authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

      SecurityContextHolder.getContext().setAuthentication(authentication);

      UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
      String jwt = jwtUtils.generateTokenFromUsername(userDetails.getUsername());

      List<String> roles = userDetails.getAuthorities().stream()
          .map(GrantedAuthority::getAuthority)
          .collect(Collectors.toList());

      return new UserInfoResponse(jwt, userDetails.getId(), userDetails.getUsername(), userDetails.getEmail(), roles);
    } catch (Exception e) {
      throw new AppException(ErrorCode.USERNAME_PASSWORD_INCORRECT);
    }
  }

  @Override
  public String registerUser(SignupRequest signUpRequest) {
    if (userRepository.existsByUsername(signUpRequest.getUsername())) {
      throw new AppException(ErrorCode.USERNAME_ALREADY_TAKEN);
    }

    if (userRepository.existsByEmail(signUpRequest.getEmail())) {
      throw new AppException(ErrorCode.EMAIL_ALREADY_IN_USE);
    }

    MilitaryPersonnelResponse createdPersonnel = militaryPersonnelService.create(signUpRequest.getMilitaryPersonnel());
    MilitaryPersonnel militaryPersonnel = militaryPersonnelRepository.findById(createdPersonnel.getId())
        .orElseThrow(() -> new AppException(ErrorCode.PERSONNEL_NOT_FOUND));

    User user = new User(signUpRequest.getUsername(), signUpRequest.getEmail(), encoder.encode(signUpRequest.getPassword()));
    user.setRoles(resolveRoles(signUpRequest.getRole()));
    user.setMilitaryPersonnel(militaryPersonnel);
    userRepository.save(user);
    return "User registered successfully!";
  }

  @Override
  public String logoutUser() {
    return "Stateless JWT does not require server-side signout.";
  }

  private Set<Role> resolveRoles(Set<String> strRoles) {
    Set<Role> roles = new HashSet<>();
    if (strRoles == null || strRoles.isEmpty()) {
      roles.add(findRoleOrThrow(ERole.ROLE_USER));
      return roles;
    }

    strRoles.forEach(role -> {
      switch (role) {
        case "admin":
        case "system_admin":
          roles.add(findRoleOrThrow(ERole.ROLE_SYSTEM_ADMIN));
          break;
        case "admin_region":
          roles.add(findRoleOrThrow(ERole.ROLE_ADMIN_REGION));
          break;
        case "admin_unit":
          roles.add(findRoleOrThrow(ERole.ROLE_ADMIN_UNIT));
          break;
        case "mod":
          roles.add(findRoleOrThrow(ERole.ROLE_MODERATOR));
          break;
        default:
          roles.add(findRoleOrThrow(ERole.ROLE_USER));
      }
    });
    return roles;
  }

  private Role findRoleOrThrow(ERole role) {
    return roleRepository.findByName(role)
        .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
  }
}
