package com.military.service.impl;

import com.military.exception.AppException;
import com.military.exception.ErrorCode;
import com.military.models.EMilitaryPosition;
import com.military.models.EMilitaryRank;
import com.military.models.MilitaryPersonnel;
import com.military.models.MilitaryRegion;
import com.military.models.MilitaryUnit;
import com.military.models.User;
import com.military.payload.response.ComboboxOptionResponse;
import com.military.repository.MilitaryPersonnelRepository;
import com.military.repository.MilitaryRegionRepository;
import com.military.repository.MilitaryUnitRepository;
import com.military.repository.UserRepository;
import com.military.security.services.UserDetailsImpl;
import com.military.service.CommonService;
import com.military.service.MilitaryPersonnelService;
import com.military.service.MilitaryRegionService;
import com.military.service.MilitaryUnitService;
import com.military.service.dto.CommonImage;
import com.military.service.dto.PersonnelImage;
import com.military.service.dto.RegionLogo;
import com.military.service.dto.UnitLogo;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CommonServiceImpl implements CommonService {
  private static final String CATEGORY_PERSONNEL = "personnel";
  private static final String CATEGORY_REGION = "region";
  private static final String CATEGORY_UNIT = "unit";

  private final MilitaryPersonnelService militaryPersonnelService;
  private final MilitaryRegionService militaryRegionService;
  private final MilitaryUnitService militaryUnitService;
  private final UserRepository userRepository;
  private final MilitaryPersonnelRepository militaryPersonnelRepository;
  private final MilitaryRegionRepository militaryRegionRepository;
  private final MilitaryUnitRepository militaryUnitRepository;

  public CommonServiceImpl(MilitaryPersonnelService militaryPersonnelService,
                           MilitaryRegionService militaryRegionService,
                           MilitaryUnitService militaryUnitService,
                           UserRepository userRepository,
                           MilitaryPersonnelRepository militaryPersonnelRepository,
                           MilitaryRegionRepository militaryRegionRepository,
                           MilitaryUnitRepository militaryUnitRepository) {
    this.militaryPersonnelService = militaryPersonnelService;
    this.militaryRegionService = militaryRegionService;
    this.militaryUnitService = militaryUnitService;
    this.userRepository = userRepository;
    this.militaryPersonnelRepository = militaryPersonnelRepository;
    this.militaryRegionRepository = militaryRegionRepository;
    this.militaryUnitRepository = militaryUnitRepository;
  }

  @Override
  public String uploadImage(String category, MultipartFile multipartFile) {
    String normalizedCategory = normalizeCategory(category);
    return switch (normalizedCategory) {
      case CATEGORY_PERSONNEL -> militaryPersonnelService.storeImage(multipartFile);
      case CATEGORY_REGION -> militaryRegionService.storeLogo(multipartFile);
      case CATEGORY_UNIT -> militaryUnitService.storeLogo(multipartFile);
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
    if (CATEGORY_REGION.equals(normalizedCategory)) {
      RegionLogo logo = militaryRegionService.loadLogo(filename);
      return new CommonImage(logo.filename(), logo.contentType(), logo.content());
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
  public List<ComboboxOptionResponse> getRegionComboboxByCurrentUser() {
    AccessScope scope = resolveAccessScope();
    List<MilitaryRegion> regions;
    if (scope.systemAdmin) {
      regions = militaryRegionRepository.findAllList();
    } else {
      MilitaryRegion currentRegion = findRegionByCode(scope.regionCode);
      regions = currentRegion == null ? List.of() : List.of(currentRegion);
    }
    return regions.stream()
        .map(region -> new ComboboxOptionResponse(region.getRegionCode(), region.getRegionName()))
        .toList();
  }

  @Override
  public List<ComboboxOptionResponse> getUnitComboboxByCurrentUser(String regionCode) {
    AccessScope scope = resolveAccessScope();
    String normalizedRegionCode = normalizeKeyword(regionCode);

    if (normalizedRegionCode != null) {
      List<MilitaryUnit> filteredUnits = filterUnitsByRegionWithScope(scope, normalizedRegionCode);
      return filteredUnits.stream()
          .map(unit -> new ComboboxOptionResponse(unit.getUnitCode(), unit.getUnitName()))
          .toList();
    }

    List<MilitaryUnit> units;
    if (scope.systemAdmin) {
      units = militaryUnitRepository.findAllList();
    } else if (scope.adminRegion) {
      units = militaryUnitRepository.findByRegionCodeIgnoreCase(scope.regionCode);
    } else {
      units = findUnitByCode(scope.unitCode).map(List::of).orElseGet(List::of);
    }
    return units.stream()
        .map(unit -> new ComboboxOptionResponse(unit.getUnitCode(), unit.getUnitName()))
        .toList();
  }

  private List<MilitaryUnit> filterUnitsByRegionWithScope(AccessScope scope, String regionCode) {
    if (scope.systemAdmin) {
      return militaryUnitRepository.findByRegionCodeIgnoreCase(regionCode);
    }
    if (scope.adminRegion) {
      if (scope.regionCode != null && scope.regionCode.equalsIgnoreCase(regionCode)) {
        return militaryUnitRepository.findByRegionCodeIgnoreCase(regionCode);
      }
      return List.of();
    }
    if (scope.adminUnit) {
      if (scope.regionCode != null && scope.regionCode.equalsIgnoreCase(regionCode)) {
        return findUnitByCode(scope.unitCode).map(List::of).orElseGet(List::of);
      }
      return List.of();
    }
    return List.of();
  }

  private String normalizeCategory(String category) {
    if (category == null || category.isBlank()) {
      throw new AppException(ErrorCode.COMMON_INVALID_FILE_CATEGORY);
    }
    String normalized = category.trim().toLowerCase();
    if (CATEGORY_PERSONNEL.equals(normalized)
        || CATEGORY_REGION.equals(normalized)
        || CATEGORY_UNIT.equals(normalized)) {
      return normalized;
    }
    throw new AppException(ErrorCode.COMMON_INVALID_FILE_CATEGORY);
  }

  private String normalizeKeyword(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private AccessScope resolveAccessScope() {
    Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    if (!(principal instanceof UserDetailsImpl userDetails)) {
      throw new AppException(ErrorCode.UNAUTHORIZED);
    }

    Set<String> roleNames = normalizeRoles(userDetails.getAuthorities());
    boolean systemAdmin = hasAnyRole(roleNames, "system_admin", "role_system_admin");
    boolean adminRegion = hasAnyRole(roleNames, "admin_region", "role_admin_region");
    boolean adminUnit = hasAnyRole(roleNames, "admin_unit", "role_admin_unit");
    if (!systemAdmin && !adminRegion && !adminUnit) {
      throw new AppException(ErrorCode.UNAUTHORIZED);
    }

    User user = userRepository.findById(userDetails.getId())
        .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED));
    if (user.getMilitaryPersonnelId() == null) {
      if (systemAdmin) {
        return new AccessScope(true, false, false, null, null);
      }
      throw new AppException(ErrorCode.PERSONNEL_NOT_FOUND);
    }

    MilitaryPersonnel personnel = militaryPersonnelRepository.findById(user.getMilitaryPersonnelId())
        .orElseThrow(() -> new AppException(ErrorCode.PERSONNEL_NOT_FOUND));
    if (personnel.getRegionCode() == null || personnel.getUnitCode() == null) {
      if (systemAdmin) {
        return new AccessScope(true, false, false, null, null);
      }
      throw new AppException(ErrorCode.PERSONNEL_NOT_FOUND);
    }
    return new AccessScope(systemAdmin, adminRegion, adminUnit, personnel.getRegionCode(), personnel.getUnitCode());
  }

  private MilitaryRegion findRegionByCode(String regionCode) {
    if (regionCode == null || regionCode.isBlank()) {
      return null;
    }
    return militaryRegionRepository.findByRegionCodeIgnoreCase(regionCode).orElse(null);
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

  private record AccessScope(boolean systemAdmin, boolean adminRegion, boolean adminUnit,
                             String regionCode, String unitCode) {
  }
}
