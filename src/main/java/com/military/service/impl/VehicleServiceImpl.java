package com.military.service.impl;

import com.military.exception.AppException;
import com.military.exception.ErrorCode;
import com.military.models.MilitaryPersonnel;
import com.military.models.Vehicle;
import com.military.payload.request.VehicleRequest;
import com.military.payload.response.VehicleResponse;
import com.military.repository.MilitaryPersonnelRepository;
import com.military.repository.UserRepository;
import com.military.repository.VehicleRepository;
import com.military.security.services.UserDetailsImpl;
import com.military.service.VehicleService;
import com.military.service.dto.VehicleImage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class VehicleServiceImpl implements VehicleService {
  private static final String IMAGE_ENDPOINT = "/api/common/images/vehicle/";
  private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
  private static final String ROLE_SYSTEM_ADMIN = "role_system_admin";
  private static final String ROLE_ADMIN_UNIT = "role_admin_unit";
  private static final String ROLE_USER = "role_user";

  private final VehicleRepository vehicleRepository;
  private final MilitaryPersonnelRepository militaryPersonnelRepository;
  private final UserRepository userRepository;
  private final S3Client s3Client;
  private final String bucketName;
  private final String objectPrefix;

  public VehicleServiceImpl(VehicleRepository vehicleRepository,
                            MilitaryPersonnelRepository militaryPersonnelRepository,
                            UserRepository userRepository,
                            S3Client s3Client,
                            @Value("${military.app.s3.bucket}") String bucketName,
                            @Value("${military.app.s3.vehicle-prefix:military-vehicles}") String objectPrefix) {
    this.vehicleRepository = vehicleRepository;
    this.militaryPersonnelRepository = militaryPersonnelRepository;
    this.userRepository = userRepository;
    this.s3Client = s3Client;
    this.bucketName = bucketName;
    this.objectPrefix = normalizePrefix(objectPrefix);
    if (this.bucketName == null || this.bucketName.trim().isEmpty()) {
      throw new IllegalStateException("military.app.s3.bucket must not be empty");
    }
  }

  @Override
  public VehicleResponse create(Long personnelId, VehicleRequest request) {
    Vehicle vehicle = new Vehicle(request);
    vehicle.setPersonnelId(personnelId);
    Vehicle saved;
    try {
      saved = vehicleRepository.createForPersonnel(vehicle);
    } catch (ConditionalCheckFailedException ex) {
      throw new AppException(ErrorCode.VEHICLE_ALREADY_EXISTS_FOR_PERSONNEL);
    }
    return toResponse(saved);
  }

  @Override
  public VehicleResponse attachToPersonnel(Long personnelId, VehicleRequest request) {
    MilitaryPersonnel targetPersonnel = militaryPersonnelRepository.findById(personnelId)
        .orElseThrow(() -> new AppException(ErrorCode.PERSONNEL_NOT_FOUND));
    AccessScope scope = resolveAccessScope();
    if (!scope.systemAdmin()) {
      boolean sameUnit = scope.adminUnit() && scope.unitCode() != null
          && scope.unitCode().equalsIgnoreCase(targetPersonnel.getUnitCode());
      boolean isSelf = scope.currentPersonnelId() != null && scope.currentPersonnelId().equals(personnelId);
      if (!sameUnit && !isSelf) {
        throw new AppException(ErrorCode.UNAUTHORIZED);
      }
    }
    return create(personnelId, request);
  }

  @Override
  public VehicleResponse update(Long id, VehicleRequest request) {
    Vehicle vehicle = findEntityById(id);
    if (!canAccessVehicle(resolveAccessScope(), vehicle)) {
      throw new AppException(ErrorCode.UNAUTHORIZED);
    }

    List<String> oldPaths = vehicle.getImagePaths();
    vehicle.setVehicleType(request.getVehicleType());
    vehicle.setBrand(request.getBrand());
    vehicle.setModel(request.getModel());
    vehicle.setLicensePlate(request.getLicensePlate());
    vehicle.setImagePaths(request.getImagePaths());

    Vehicle saved = vehicleRepository.save(vehicle);

    if (oldPaths != null) {
      List<String> newPaths = saved.getImagePaths() == null ? List.of() : saved.getImagePaths();
      for (String oldPath : oldPaths) {
        if (!newPaths.contains(oldPath)) {
          deleteObjectIfExists(oldPath);
        }
      }
    }

    return toResponse(saved);
  }

  @Override
  public VehicleResponse getById(Long id) {
    Vehicle vehicle = findEntityById(id);
    if (!canAccessVehicle(resolveAccessScope(), vehicle)) {
      throw new AppException(ErrorCode.UNAUTHORIZED);
    }
    return toResponse(vehicle);
  }

  @Override
  public VehicleResponse getByPersonnelId(Long personnelId) {
    Vehicle vehicle = vehicleRepository.findByPersonnelId(personnelId)
        .orElseThrow(() -> new AppException(ErrorCode.VEHICLE_NOT_FOUND));
    if (!canAccessVehicle(resolveAccessScope(), vehicle)) {
      throw new AppException(ErrorCode.UNAUTHORIZED);
    }
    return toResponse(vehicle);
  }

  @Override
  public Page<VehicleResponse> list(String keyword, Pageable pageable) {
    AccessScope scope = resolveAccessScope();
    String cleanKeyword = keyword == null ? null : keyword.trim();
    if (cleanKeyword != null && cleanKeyword.isEmpty()) {
      cleanKeyword = null;
    }
    final String keywordForFilter = cleanKeyword;

    List<Vehicle> filtered = vehicleRepository.findAllList().stream()
        .filter(item -> canAccessVehicle(scope, item))
        .filter(item -> matchesKeyword(item, keywordForFilter))
        .toList();

    int start = (int) pageable.getOffset();
    if (start >= filtered.size()) {
      return new PageImpl<>(List.of(), pageable, filtered.size());
    }
    int end = Math.min(start + pageable.getPageSize(), filtered.size());
    return new PageImpl<>(filtered.subList(start, end), pageable, filtered.size()).map(this::toResponse);
  }

  @Override
  public void delete(Long id) {
    Vehicle vehicle = findEntityById(id);
    AccessScope scope = resolveAccessScope();
    if (!scope.systemAdmin() && !(scope.adminUnit() && canAccessVehicle(scope, vehicle))) {
      throw new AppException(ErrorCode.UNAUTHORIZED);
    }
    deleteAllImages(vehicle.getImagePaths());
    vehicleRepository.delete(vehicle);
  }

  @Override
  public void deleteByPersonnelId(Long personnelId) {
    vehicleRepository.findByPersonnelId(personnelId).ifPresent(vehicle -> {
      deleteAllImages(vehicle.getImagePaths());
      vehicleRepository.delete(vehicle);
    });
  }

  @Override
  public void deleteImage(Long id, String imagePath) {
    Vehicle vehicle = findEntityById(id);
    if (!canAccessVehicle(resolveAccessScope(), vehicle)) {
      throw new AppException(ErrorCode.UNAUTHORIZED);
    }
    List<String> paths = vehicle.getImagePaths();
    if (paths == null || !paths.contains(imagePath)) {
      throw new AppException(ErrorCode.VEHICLE_IMAGE_NOT_FOUND);
    }
    List<String> updatedPaths = paths.stream().filter(p -> !p.equals(imagePath)).collect(Collectors.toList());
    vehicle.setImagePaths(updatedPaths);
    vehicleRepository.save(vehicle);
    deleteObjectIfExists(imagePath);
  }

  @Override
  public String storeImage(MultipartFile imageFile) {
    if (imageFile == null || imageFile.isEmpty()) {
      return null;
    }

    String extension = extractExtension(imageFile.getOriginalFilename());
    String fileName = UUID.randomUUID() + extension;
    String key = buildObjectKey(fileName);

    try {
      String contentType = imageFile.getContentType();
      if (contentType == null || contentType.isBlank()) {
        contentType = DEFAULT_CONTENT_TYPE;
      }
      s3Client.putObject(
          PutObjectRequest.builder()
              .bucket(bucketName)
              .key(key)
              .contentType(contentType)
              .build(),
          RequestBody.fromInputStream(imageFile.getInputStream(), imageFile.getSize())
      );
      return fileName;
    } catch (IOException | S3Exception ex) {
      throw new AppException(ErrorCode.VEHICLE_IMAGE_SAVE_FAILED);
    }
  }

  @Override
  public VehicleImage loadImage(String filename) {
    if (filename == null || filename.isBlank()) {
      throw new AppException(ErrorCode.VEHICLE_NOT_FOUND);
    }
    String key = buildObjectKey(filename);
    try {
      ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(
          GetObjectRequest.builder()
              .bucket(bucketName)
              .key(key)
              .build()
      );
      String contentType = objectBytes.response().contentType();
      if (contentType == null || contentType.isBlank()) {
        contentType = DEFAULT_CONTENT_TYPE;
      }
      return new VehicleImage(filename, contentType, objectBytes.asByteArray());
    } catch (NoSuchKeyException ex) {
      throw new AppException(ErrorCode.VEHICLE_NOT_FOUND);
    } catch (S3Exception ex) {
      if (ex.statusCode() == 404) {
        throw new AppException(ErrorCode.VEHICLE_NOT_FOUND);
      }
      throw new AppException(ErrorCode.VEHICLE_IMAGE_SAVE_FAILED);
    }
  }

  private Vehicle findEntityById(Long id) {
    return vehicleRepository.findById(id)
        .orElseThrow(() -> new AppException(ErrorCode.VEHICLE_NOT_FOUND));
  }

  private boolean canAccessVehicle(AccessScope scope, Vehicle vehicle) {
    if (scope.systemAdmin()) {
      return true;
    }
    if (scope.adminUnit()) {
      MilitaryPersonnel owner = militaryPersonnelRepository.findById(vehicle.getPersonnelId()).orElse(null);
      return owner != null && scope.unitCode() != null && scope.unitCode().equalsIgnoreCase(owner.getUnitCode());
    }
    return scope.currentPersonnelId() != null && scope.currentPersonnelId().equals(vehicle.getPersonnelId());
  }

  private boolean matchesKeyword(Vehicle vehicle, String keyword) {
    if (keyword == null) {
      return true;
    }
    String keywordLower = keyword.toLowerCase(Locale.ROOT);
    return containsIgnoreCase(vehicle.getLicensePlate(), keywordLower)
        || containsIgnoreCase(vehicle.getBrand(), keywordLower)
        || containsIgnoreCase(vehicle.getModel(), keywordLower);
  }

  private boolean containsIgnoreCase(String value, String keywordLower) {
    return value != null && keywordLower != null && value.toLowerCase(Locale.ROOT).contains(keywordLower);
  }

  private void deleteAllImages(List<String> imagePaths) {
    if (imagePaths == null) {
      return;
    }
    imagePaths.forEach(this::deleteObjectIfExists);
  }

  private void deleteObjectIfExists(String path) {
    if (path == null || path.isBlank()) {
      return;
    }
    try {
      s3Client.deleteObject(
          DeleteObjectRequest.builder()
              .bucket(bucketName)
              .key(buildObjectKey(path))
              .build()
      );
    } catch (NoSuchKeyException ignored) {
      return;
    } catch (S3Exception ex) {
      if (ex.statusCode() == 404) {
        return;
      }
      throw new AppException(ErrorCode.VEHICLE_IMAGE_DELETE_FAILED);
    }
  }

  private String extractExtension(String fileName) {
    if (fileName == null) {
      return ".jpg";
    }
    int dotIndex = fileName.lastIndexOf('.');
    if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
      return ".jpg";
    }
    return fileName.substring(dotIndex);
  }

  private String buildObjectKey(String fileName) {
    return objectPrefix + fileName;
  }

  private String normalizePrefix(String rawPrefix) {
    if (rawPrefix == null || rawPrefix.isBlank()) {
      return "";
    }
    String normalized = rawPrefix.trim();
    normalized = normalized.replaceAll("^/+", "");
    normalized = normalized.replaceAll("/+$", "");
    if (normalized.isEmpty()) {
      return "";
    }
    return normalized + "/";
  }

  private VehicleResponse toResponse(Vehicle vehicle) {
    VehicleResponse response = new VehicleResponse(vehicle);
    response.setImageUrls(vehicle.getImagePaths() == null ? List.of()
        : vehicle.getImagePaths().stream().map(p -> IMAGE_ENDPOINT + p).toList());
    return response;
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
    boolean systemAdmin = roleNames.contains(ROLE_SYSTEM_ADMIN);
    boolean adminUnit = roleNames.contains(ROLE_ADMIN_UNIT);
    boolean userRole = roleNames.contains(ROLE_USER);

    if (!systemAdmin && !adminUnit && !userRole) {
      throw new AppException(ErrorCode.UNAUTHORIZED);
    }

    Long currentPersonnelId = null;
    String unitCode = null;

    if (!systemAdmin) {
      com.military.models.User user = userRepository.findById(userDetails.getId())
          .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED));
      currentPersonnelId = user.getMilitaryPersonnelId();
      if (currentPersonnelId == null) {
        throw new AppException(ErrorCode.PERSONNEL_NOT_FOUND);
      }
      MilitaryPersonnel currentPersonnel = militaryPersonnelRepository.findById(currentPersonnelId)
          .orElseThrow(() -> new AppException(ErrorCode.PERSONNEL_NOT_FOUND));
      unitCode = currentPersonnel.getUnitCode();
      if (adminUnit && unitCode == null) {
        throw new AppException(ErrorCode.PERSONNEL_NOT_FOUND);
      }
    }

    return new AccessScope(systemAdmin, adminUnit, userRole, currentPersonnelId, unitCode);
  }

  private Set<String> normalizeRoles(Collection<? extends GrantedAuthority> authorities) {
    return authorities.stream()
        .map(GrantedAuthority::getAuthority)
        .filter(value -> value != null && !value.isBlank())
        .map(value -> value.toLowerCase(Locale.ROOT))
        .collect(Collectors.toSet());
  }

  private record AccessScope(boolean systemAdmin, boolean adminUnit, boolean userRole,
                             Long currentPersonnelId, String unitCode) {
  }
}
