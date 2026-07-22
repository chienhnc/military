package com.military.service.impl;

import com.military.exception.AppException;
import com.military.exception.ErrorCode;
import com.military.models.EMilitaryPosition;
import com.military.models.EMilitaryRank;
import com.military.models.MilitaryPersonnel;
import com.military.models.MilitaryUnit;
import com.military.models.User;
import com.military.payload.response.ComboboxOptionResponse;
import com.military.repository.MilitaryPersonnelRepository;
import com.military.repository.MilitaryUnitRepository;
import com.military.repository.UserRepository;
import com.military.security.services.UserDetailsImpl;
import com.military.service.CommonService;
import com.military.service.MilitaryPersonnelService;
import com.military.service.MilitaryUnitService;
import com.military.service.VehicleService;
import com.military.service.dto.CommonImage;
import com.military.service.dto.PersonnelImage;
import com.military.service.dto.UnitLogo;
import com.military.service.dto.VehicleImage;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CommonServiceImpl implements CommonService {
  private static final String CATEGORY_PERSONNEL = "personnel";
  private static final String CATEGORY_UNIT = "unit";
  private static final String CATEGORY_VEHICLE = "vehicle";

  private final MilitaryPersonnelService militaryPersonnelService;
  private final MilitaryUnitService militaryUnitService;
  private final VehicleService vehicleService;
  private final UserRepository userRepository;
  private final MilitaryPersonnelRepository militaryPersonnelRepository;
  private final MilitaryUnitRepository militaryUnitRepository;

  public CommonServiceImpl(MilitaryPersonnelService militaryPersonnelService,
                           MilitaryUnitService militaryUnitService,
                           VehicleService vehicleService,
                           UserRepository userRepository,
                           MilitaryPersonnelRepository militaryPersonnelRepository,
                           MilitaryUnitRepository militaryUnitRepository) {
    this.militaryPersonnelService = militaryPersonnelService;
    this.militaryUnitService = militaryUnitService;
    this.vehicleService = vehicleService;
    this.userRepository = userRepository;
    this.militaryPersonnelRepository = militaryPersonnelRepository;
    this.militaryUnitRepository = militaryUnitRepository;
  }

  @Override
  public String uploadImage(String category, MultipartFile multipartFile) {
    String normalizedCategory = normalizeCategory(category);
    return switch (normalizedCategory) {
      case CATEGORY_PERSONNEL -> militaryPersonnelService.storeImage(multipartFile);
      case CATEGORY_UNIT -> militaryUnitService.storeLogo(multipartFile);
      case CATEGORY_VEHICLE -> vehicleService.storeImage(multipartFile);
      default -> throw new AppException(ErrorCode.COMMON_INVALID_FILE_CATEGORY);
    };
  }

  @Override
  public CommonImage loadImage(String category, String filename) {
    String normalizedCategory = normalizeCategory(category);
    if (CATEGORY_PERSONNEL.equals(normalizedCategory)) {
      PersonnelImage image = militaryPersonnelService.loadImage(filename);
      return new CommonImage(image.filename(), image.contentType(), image.content());
    }
    if (CATEGORY_VEHICLE.equals(normalizedCategory)) {
      VehicleImage image = vehicleService.loadImage(filename);
      return new CommonImage(image.filename(), image.contentType(), image.content());
    }
    UnitLogo logo = militaryUnitService.loadLogo(filename);
    return new CommonImage(logo.filename(), logo.contentType(), logo.content());
  }

  @Override
  public List<ComboboxOptionResponse> getRankCombobox() {
    return Arrays.stream(EMilitaryRank.values())
        .map(rank -> new ComboboxOptionResponse(rank.getCode(), rank.getName()))
        .toList();
  }

  @Override
  public List<ComboboxOptionResponse> getPositionCombobox() {
    return Arrays.stream(EMilitaryPosition.values())
        .map(position -> new ComboboxOptionResponse(position.getCode(), position.getName()))
        .toList();
  }

  @Override
  public List<ComboboxOptionResponse> getUnitComboboxByCurrentUser() {
    AccessScope scope = resolveAccessScope();
    List<MilitaryUnit> units;
    if (scope.systemAdmin) {
      units = militaryUnitRepository.findAllList();
    } else {
      units = findUnitByCode(scope.unitCode).map(List::of).orElseGet(List::of);
    }
    return units.stream()
        .map(unit -> new ComboboxOptionResponse(unit.getUnitCode(), unit.getUnitName()))
        .toList();
  }

  @Override
  public List<ComboboxOptionResponse> getUserCombobox() {
    Map<Long, MilitaryPersonnel> personnelById = militaryPersonnelRepository.findAllList().stream()
        .collect(Collectors.toMap(MilitaryPersonnel::getId, personnel -> personnel, (a, b) -> a));
    return userRepository.findAllList().stream()
        .filter(user -> user.getMilitaryPersonnelId() != null
            && personnelById.containsKey(user.getMilitaryPersonnelId()))
        .map(user -> {
          MilitaryPersonnel personnel = personnelById.get(user.getMilitaryPersonnelId());
          String name = personnel.getFullName() + " (" + user.getUsername() + ")";
          return new ComboboxOptionResponse(String.valueOf(user.getId()), name);
        })
        .toList();
  }

  @Override
  public List<ComboboxOptionResponse> getMilitaryPersonnelCombobox() {
    Map<Long, MilitaryPersonnel> personnelById = militaryPersonnelRepository.findAllList().stream()
            .collect(Collectors.toMap(MilitaryPersonnel::getId, personnel -> personnel, (a, b) -> a));
    return militaryPersonnelRepository.findAllList().stream()
            .map(user -> {
              return new ComboboxOptionResponse(String.valueOf(user.getId()), user.getFullName());
            })
            .toList();
  }

  private String normalizeCategory(String category) {
    if (category == null || category.isBlank()) {
      throw new AppException(ErrorCode.COMMON_INVALID_FILE_CATEGORY);
    }
    String normalized = category.trim().toLowerCase();
    if (CATEGORY_PERSONNEL.equals(normalized)
        || CATEGORY_UNIT.equals(normalized)
        || CATEGORY_VEHICLE.equals(normalized)) {
      return normalized;
    }
    throw new AppException(ErrorCode.COMMON_INVALID_FILE_CATEGORY);
  }

  private AccessScope resolveAccessScope() {
    Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    if (!(principal instanceof UserDetailsImpl userDetails)) {
      throw new AppException(ErrorCode.UNAUTHORIZED);
    }

    Set<String> roleNames = normalizeRoles(userDetails.getAuthorities());
    boolean systemAdmin = hasAnyRole(roleNames, "system_admin", "role_system_admin");
    boolean adminUnit = hasAnyRole(roleNames, "admin_unit", "role_admin_unit");
    if (!systemAdmin && !adminUnit) {
      throw new AppException(ErrorCode.UNAUTHORIZED);
    }

    User user = userRepository.findById(userDetails.getId())
        .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED));
    if (user.getMilitaryPersonnelId() == null) {
      if (systemAdmin) {
        return new AccessScope(true, false, null);
      }
      throw new AppException(ErrorCode.PERSONNEL_NOT_FOUND);
    }

    MilitaryPersonnel personnel = militaryPersonnelRepository.findById(user.getMilitaryPersonnelId())
        .orElseThrow(() -> new AppException(ErrorCode.PERSONNEL_NOT_FOUND));
    if (personnel.getUnitCode() == null) {
      if (systemAdmin) {
        return new AccessScope(true, false, null);
      }
      throw new AppException(ErrorCode.PERSONNEL_NOT_FOUND);
    }
    return new AccessScope(systemAdmin, adminUnit, personnel.getUnitCode());
  }

  private java.util.Optional<MilitaryUnit> findUnitByCode(String unitCode) {
    if (unitCode == null || unitCode.isBlank()) {
      return java.util.Optional.empty();
    }
    return militaryUnitRepository.findByUnitCodeIgnoreCase(unitCode);
  }

  private Set<String> normalizeRoles(Collection<? extends GrantedAuthority> authorities) {
    return authorities.stream()
        .map(GrantedAuthority::getAuthority)
        .filter(value -> value != null && !value.isBlank())
        .map(value -> value.toLowerCase(Locale.ROOT))
        .collect(Collectors.toSet());
  }

  private boolean hasAnyRole(Set<String> roleNames, String... candidates) {
    for (String candidate : candidates) {
      if (roleNames.contains(candidate)) {
        return true;
      }
    }
    return false;
  }

  private record AccessScope(boolean systemAdmin, boolean adminUnit, String unitCode) {
  }
}
