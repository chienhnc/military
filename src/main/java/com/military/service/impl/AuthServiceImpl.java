package com.military.service.impl;

import com.military.exception.AppException;
import com.military.exception.ErrorCode;
import com.military.models.ERole;
import com.military.models.MilitaryUnit;
import com.military.models.MilitaryPersonnel;
import com.military.models.Role;
import com.military.models.User;
import com.military.payload.request.LoginRequest;
import com.military.payload.request.SignupRequest;
import com.military.payload.response.MilitaryPersonnelResponse;
import com.military.payload.response.UserInfoResponse;
import com.military.repository.MilitaryPersonnelRepository;
import com.military.repository.MilitaryUnitRepository;
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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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
  private final MilitaryUnitRepository militaryUnitRepository;
  private final MilitaryPersonnelService militaryPersonnelService;

  public AuthServiceImpl(AuthenticationManager authenticationManager,
                         UserRepository userRepository,
                         RoleRepository roleRepository,
                         PasswordEncoder encoder,
                         JwtUtils jwtUtils,
                         MilitaryPersonnelRepository militaryPersonnelRepository,
                         MilitaryUnitRepository militaryUnitRepository,
                         MilitaryPersonnelService militaryPersonnelService) {
    this.authenticationManager = authenticationManager;
    this.userRepository = userRepository;
    this.roleRepository = roleRepository;
    this.encoder = encoder;
    this.jwtUtils = jwtUtils;
    this.militaryPersonnelRepository = militaryPersonnelRepository;
    this.militaryUnitRepository = militaryUnitRepository;
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
    AccessScope scope = resolveAccessScope();
    MilitaryUnit targetUnit = resolveTargetUnit(signUpRequest);
    authorizeSignup(scope, targetUnit);

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

  private MilitaryUnit resolveTargetUnit(SignupRequest signUpRequest) {
    if (signUpRequest.getMilitaryPersonnel() == null || signUpRequest.getMilitaryPersonnel().getUnitCode() == null) {
      throw new AppException(ErrorCode.MILITARY_UNIT_NOT_FOUND);
    }
    return militaryUnitRepository.findByUnitCodeIgnoreCase(signUpRequest.getMilitaryPersonnel().getUnitCode())
        .orElseThrow(() -> new AppException(ErrorCode.MILITARY_UNIT_NOT_FOUND));
  }

  private void authorizeSignup(AccessScope scope, MilitaryUnit targetUnit) {
    if (scope.systemAdmin()) {
      return;
    }
    if (scope.adminRegion()) {
      if (scope.regionCode() == null || targetUnit.getRegionCode() == null
          || !scope.regionCode().equalsIgnoreCase(targetUnit.getRegionCode())) {
        throw new AppException(ErrorCode.UNAUTHORIZED);
      }
      return;
    }
    if (scope.adminUnit()) {
      if (scope.unitCode() == null || targetUnit.getUnitCode() == null
          || !scope.unitCode().equalsIgnoreCase(targetUnit.getUnitCode())) {
        throw new AppException(ErrorCode.UNAUTHORIZED);
      }
      return;
    }
    throw new AppException(ErrorCode.UNAUTHORIZED);
  }

  private AccessScope resolveAccessScope() {
    if (SecurityContextHolder.getContext().getAuthentication() == null) {
      throw new AppException(ErrorCode.UNAUTHORIZED);
    }
    Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    if (!(principal instanceof UserDetailsImpl userDetails)) {
      throw new AppException(ErrorCode.UNAUTHORIZED);
    }

    Set<String> roleNames = normalizeRoles(userDetails.getAuthorities());
    boolean systemAdmin = roleNames.contains("role_system_admin");
    boolean adminRegion = roleNames.contains("role_admin_region");
    boolean adminUnit = roleNames.contains("role_admin_unit");
    boolean userRole = roleNames.contains("role_user");

    if (!systemAdmin && !adminRegion && !adminUnit && !userRole) {
      throw new AppException(ErrorCode.UNAUTHORIZED);
    }
    if (userRole && !systemAdmin && !adminRegion && !adminUnit) {
      throw new AppException(ErrorCode.UNAUTHORIZED);
    }

    if (systemAdmin) {
      return new AccessScope(true, false, false, false, null, null);
    }

    User actor = userRepository.findById(userDetails.getId())
        .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED));
    Long personnelId = actor.getMilitaryPersonnelId();
    if (personnelId == null) {
      throw new AppException(ErrorCode.PERSONNEL_NOT_FOUND);
    }
    MilitaryPersonnel personnel = militaryPersonnelRepository.findById(personnelId)
        .orElseThrow(() -> new AppException(ErrorCode.PERSONNEL_NOT_FOUND));

    String regionCode = personnel.getRegionCode();
    String unitCode = personnel.getUnitCode();

    if (adminRegion && (regionCode == null || regionCode.isBlank())) {
      throw new AppException(ErrorCode.PERSONNEL_NOT_FOUND);
    }
    if (adminUnit && (unitCode == null || unitCode.isBlank())) {
      throw new AppException(ErrorCode.PERSONNEL_NOT_FOUND);
    }

    return new AccessScope(false, adminRegion, adminUnit, userRole, regionCode, unitCode);
  }

  private Set<String> normalizeRoles(Collection<? extends GrantedAuthority> authorities) {
    return authorities.stream()
        .map(GrantedAuthority::getAuthority)
        .filter(value -> value != null && !value.isBlank())
        .map(value -> value.toLowerCase(Locale.ROOT))
        .collect(Collectors.toSet());
  }

  private record AccessScope(boolean systemAdmin, boolean adminRegion, boolean adminUnit, boolean userRole,
                             String regionCode, String unitCode) {
  }
}
