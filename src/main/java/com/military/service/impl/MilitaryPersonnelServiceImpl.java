package com.military.service.impl;

import com.google.gson.Gson;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.military.exception.AppException;
import com.military.exception.ErrorCode;
import com.military.models.EMilitaryPosition;
import com.military.models.EMilitaryRank;
import com.military.models.EQrSource;
import com.military.models.MilitaryPersonnel;
import com.military.models.MilitaryUnit;
import com.military.payload.request.MilitaryPersonnelRequest;
import com.military.payload.response.MilitaryPersonnelResponse;
import com.military.repository.MilitaryPersonnelRepository;
import com.military.repository.MilitaryUnitRepository;
import com.military.repository.UserRepository;
import com.military.security.services.UserDetailsImpl;
import com.military.service.MilitaryPersonnelService;
import com.military.service.dto.PersonnelImage;
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
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class MilitaryPersonnelServiceImpl implements MilitaryPersonnelService {
  private static final Pattern NON_ASCII = Pattern.compile("[^\\p{ASCII}]");
  private static final Pattern NON_ALNUM = Pattern.compile("[^A-Za-z0-9]+");
  private static final Pattern MULTI_DASH = Pattern.compile("-+");
  private static final int QR_SIZE = 300;
  private static final String IMAGE_ENDPOINT = "/api/common/images/personnel/";
  private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
  private static final String ROLE_SYSTEM_ADMIN = "role_system_admin";
  private static final String ROLE_ADMIN_REGION = "role_admin_region";
  private static final String ROLE_ADMIN_UNIT = "role_admin_unit";
  private static final String ROLE_USER = "role_user";

  private final MilitaryPersonnelRepository militaryPersonnelRepository;
  private final MilitaryUnitRepository militaryUnitRepository;
  private final UserRepository userRepository;
  private final S3Client s3Client;
  private final String bucketName;
  private final String objectPrefix;
  private final Gson gson;

  public MilitaryPersonnelServiceImpl(MilitaryPersonnelRepository militaryPersonnelRepository,
                                      MilitaryUnitRepository militaryUnitRepository,
                                      UserRepository userRepository,
                                      S3Client s3Client,
                                      @Value("${military.app.s3.bucket}") String bucketName,
                                      @Value("${military.app.s3.prefix:personnel}") String objectPrefix,
                                      Gson gson) {
    this.militaryPersonnelRepository = militaryPersonnelRepository;
    this.militaryUnitRepository = militaryUnitRepository;
    this.userRepository = userRepository;
    this.s3Client = s3Client;
    this.bucketName = bucketName;
    this.objectPrefix = normalizePrefix(objectPrefix);
    this.gson = gson;
    if (this.bucketName == null || this.bucketName.trim().isEmpty()) {
      throw new IllegalStateException("military.app.s3.bucket must not be empty");
    }
  }

  public MilitaryPersonnelResponse create(MilitaryPersonnelRequest militaryPersonnelRequest) {
    AccessScope scope = resolveAccessScopeIfPresent();
    MilitaryUnit unit = resolveUnitByCode(militaryPersonnelRequest.getUnitCode());
    authorizeCreate(scope, unit);

    MilitaryPersonnel personnel = new MilitaryPersonnel(militaryPersonnelRequest);
    personnel.setUnitCode(unit.getUnitCode());
    personnel.setRegionCode(unit.getRegionCode());
    personnel.setCode(generateCode(unit.getRegionCode(), unit.getUnitCode(), militaryPersonnelRequest.getRankCode(),
        militaryPersonnelRequest.getPositionCode()));
    applySystemQrCode(personnel);

    MilitaryPersonnel saved = militaryPersonnelRepository.save(personnel);
    return toResponse(saved);
  }

  public MilitaryPersonnelResponse update(Long id,
                                          MilitaryPersonnelRequest militaryPersonnelRequest) {
    AccessScope scope = resolveAccessScope();
    MilitaryPersonnel personnel = findEntityById(id);
    authorizeUpdate(scope, personnel, militaryPersonnelRequest);
    MilitaryPersonnel saved = militaryPersonnelRepository.save(personnel);
    return toResponse(saved);
  }

  public MilitaryPersonnelResponse getById(Long id) {
    MilitaryPersonnel personnel = findEntityById(id);
    AccessScope scope = resolveAccessScope();
    if (!canAccessPersonnel(scope, personnel)) {
      throw new AppException(ErrorCode.PERSONNEL_NOT_FOUND);
    }
    return toResponse(personnel);
  }

  public Page<MilitaryPersonnelResponse> list(String keyword, Pageable pageable) {
    AccessScope scope = resolveAccessScope();
    String cleanKeyword = keyword == null ? null : keyword.trim();
    if (cleanKeyword != null && cleanKeyword.isEmpty()) {
      cleanKeyword = null;
    }
    final String keywordForFilter = cleanKeyword;

    List<MilitaryPersonnel> filtered = militaryPersonnelRepository.findAllList().stream()
        .filter(item -> canAccessPersonnel(scope, item))
        .filter(item -> matchesKeyword(item, keywordForFilter))
        .toList();

    int start = (int) pageable.getOffset();
    if (start >= filtered.size()) {
      return new PageImpl<>(List.of(), pageable, filtered.size());
    }
    int end = Math.min(start + pageable.getPageSize(), filtered.size());
    return new PageImpl<>(filtered.subList(start, end), pageable, filtered.size()).map(this::toResponse);
  }

  public void delete(Long id) {
    AccessScope scope = resolveAccessScope();
    if (scope.userRole() && !scope.systemAdmin() && !scope.adminRegion() && !scope.adminUnit()) {
      throw new AppException(ErrorCode.UNAUTHORIZED);
    }
    MilitaryPersonnel personnel = findEntityById(id);
    if (!scope.systemAdmin() && !canAccessPersonnel(scope, personnel)) {
      throw new AppException(ErrorCode.UNAUTHORIZED);
    }
    deleteImageIfExists(personnel.getImagePath());
    militaryPersonnelRepository.delete(personnel);
  }

  public PersonnelImage loadImage(String filename) {
    if (filename == null || filename.isBlank()) {
      throw new AppException(ErrorCode.PERSONNEL_NOT_FOUND);
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
      return new PersonnelImage(filename, contentType, objectBytes.asByteArray());
    } catch (NoSuchKeyException ex) {
      throw new AppException(ErrorCode.PERSONNEL_NOT_FOUND);
    } catch (S3Exception ex) {
      if (ex.statusCode() == 404) {
        throw new AppException(ErrorCode.PERSONNEL_NOT_FOUND);
      }
      throw new AppException(ErrorCode.PERSONNEL_IMAGE_SAVE_FAILED);
    }
  }

  private MilitaryPersonnel findEntityById(Long id) {
    return militaryPersonnelRepository.findById(id)
        .orElseThrow(() -> new AppException(ErrorCode.PERSONNEL_NOT_FOUND));
  }

  private boolean isPrefixChanged(MilitaryPersonnel personnel, String unitCode, EMilitaryRank rank,
                                  EMilitaryPosition position) {
    return !Objects.equals(personnel.getUnitCode(), unitCode)
        || personnel.getRankCode() != rank
        || !Objects.equals(personnel.getPositionCode(), position);
  }

  private synchronized String generateCode(String regionCode,
                                           String unitCode,
                                           EMilitaryRank rankCode,
                                           EMilitaryPosition positionCode) {
    String prefix = buildPrefix(regionCode, unitCode, rankCode, positionCode);
    Optional<MilitaryPersonnel> latest = militaryPersonnelRepository
        .findFirstByCodeStartingWithOrderByCodeDesc(prefix);

    int nextNumber = 1;
    if (latest.isPresent()) {
      String latestCode = latest.get().getCode();
      if (latestCode != null && latestCode.startsWith(prefix) && latestCode.length() > prefix.length()) {
        String sequenceStr = latestCode.substring(prefix.length());
        try {
          nextNumber = Integer.parseInt(sequenceStr) + 1;
        } catch (NumberFormatException ignored) {
          nextNumber = 1;
        }
      }
    }

    String code;
    do {
      code = prefix + "|" + String.format("%05d", nextNumber++);
    } while (militaryPersonnelRepository.existsByCode(code));

    return code;
  }

  private String buildPrefix(String regionCode, String unitCode, EMilitaryRank rank, EMilitaryPosition position) {
    String regionSegment = sanitizeForCode(regionCode);
    String unitSegment = sanitizeForCode(unitCode);
    String rankSegment = sanitizeForCode(rank == null ? null : rank.getCode());
    String positionSegment = sanitizeForCode(position == null ? null : position.getCode());
    return regionSegment + "|" + unitSegment + "|" + rankSegment + "|" + positionSegment;
  }

  private String sanitizeForCode(String value) {
    if (value == null || value.isBlank()) {
      throw new AppException(ErrorCode.PERSONNEL_INVALID_INPUT);
    }
    String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
    String ascii = NON_ASCII.matcher(normalized).replaceAll("");
    String slug = NON_ALNUM.matcher(ascii).replaceAll("-");
    slug = MULTI_DASH.matcher(slug).replaceAll("-");
    slug = slug.replaceAll("^-|-$", "");
    if (slug.isEmpty()) {
      throw new AppException(ErrorCode.PERSONNEL_INVALID_INPUT);
    }
    return slug.toUpperCase();
  }

  private MilitaryUnit resolveUnitByCode(String unitCode) {
    return militaryUnitRepository.findByUnitCodeIgnoreCase(unitCode)
        .orElseThrow(() -> new AppException(ErrorCode.MILITARY_UNIT_NOT_FOUND));
  }

  private String generateQrCode(MilitaryPersonnel personnel) {
    String content = gson.toJson(personnel);

    QRCodeWriter writer = new QRCodeWriter();
    try {
      BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE);
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
      return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    } catch (WriterException | IOException ex) {
      throw new AppException(ErrorCode.PERSONNEL_QR_GENERATE_FAILED);
    }
  }

  private void applySystemQrCode(MilitaryPersonnel personnel) {
    personnel.setQrCode(generateQrCode(personnel));
    personnel.setQrSource(EQrSource.SYSTEM);
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
    boolean adminRegion = roleNames.contains(ROLE_ADMIN_REGION);
    boolean adminUnit = roleNames.contains(ROLE_ADMIN_UNIT);
    boolean userRole = roleNames.contains(ROLE_USER);

    if (!systemAdmin && !adminRegion && !adminUnit && !userRole) {
      throw new AppException(ErrorCode.UNAUTHORIZED);
    }

    Long currentPersonnelId = null;
    String regionCode = null;
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
      regionCode = currentPersonnel.getRegionCode();
      unitCode = currentPersonnel.getUnitCode();
      if ((adminRegion || adminUnit) && (regionCode == null || unitCode == null)) {
        throw new AppException(ErrorCode.PERSONNEL_NOT_FOUND);
      }
    }

    return new AccessScope(systemAdmin, adminRegion, adminUnit, userRole, currentPersonnelId, regionCode, unitCode);
  }

  private AccessScope resolveAccessScopeIfPresent() {
    if (SecurityContextHolder.getContext().getAuthentication() == null) {
      return null;
    }
    Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    if (!(principal instanceof UserDetailsImpl)) {
      return null;
    }
    return resolveAccessScope();
  }

  private void authorizeCreate(AccessScope scope, MilitaryUnit targetUnit) {
    if (scope == null || scope.systemAdmin()) {
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

  private void authorizeUpdate(AccessScope scope, MilitaryPersonnel personnel, MilitaryPersonnelRequest request) {
    if (scope.systemAdmin()) {
      applyFullUpdate(personnel, request);
      return;
    }

    if (scope.adminRegion() || scope.adminUnit()) {
      if (!canAccessPersonnel(scope, personnel)) {
        throw new AppException(ErrorCode.UNAUTHORIZED);
      }
      MilitaryUnit targetUnit = resolveUnitByCode(request.getUnitCode());
      if (scope.adminRegion()
          && (scope.regionCode() == null || !scope.regionCode().equalsIgnoreCase(targetUnit.getRegionCode()))) {
        throw new AppException(ErrorCode.UNAUTHORIZED);
      }
      if (scope.adminUnit()
          && (scope.unitCode() == null || !scope.unitCode().equalsIgnoreCase(targetUnit.getUnitCode()))) {
        throw new AppException(ErrorCode.UNAUTHORIZED);
      }
      applyFullUpdate(personnel, request);
      return;
    }

    if (scope.userRole()) {
      if (scope.currentPersonnelId() == null || !scope.currentPersonnelId().equals(personnel.getId())) {
        throw new AppException(ErrorCode.UNAUTHORIZED);
      }
      personnel.setImagePath(request.getImagePath());
      applySystemQrCode(personnel);
      return;
    }

    throw new AppException(ErrorCode.UNAUTHORIZED);
  }

  private void applyFullUpdate(MilitaryPersonnel personnel, MilitaryPersonnelRequest request) {
    MilitaryUnit unit = resolveUnitByCode(request.getUnitCode());
    personnel.setFullName(request.getFullName());
    boolean shouldRegenerateCode = isPrefixChanged(personnel, unit.getUnitCode(),
        request.getRankCode(), request.getPositionCode());
    personnel.setRegionCode(unit.getRegionCode());
    personnel.setRankCode(request.getRankCode());
    personnel.setUnitCode(unit.getUnitCode());
    personnel.setPositionCode(request.getPositionCode());
    if (shouldRegenerateCode) {
      personnel.setCode(generateCode(unit.getRegionCode(), unit.getUnitCode(), request.getRankCode(),
          request.getPositionCode()));
    }
    personnel.setImagePath(request.getImagePath());
    applySystemQrCode(personnel);
  }

  private Set<String> normalizeRoles(Collection<? extends GrantedAuthority> authorities) {
    return authorities.stream()
        .map(GrantedAuthority::getAuthority)
        .filter(value -> value != null && !value.isBlank())
        .map(value -> value.toLowerCase(Locale.ROOT))
        .collect(Collectors.toSet());
  }

  private boolean canAccessPersonnel(AccessScope scope, MilitaryPersonnel personnel) {
    if (scope.systemAdmin()) {
      return true;
    }
    if (scope.adminRegion()) {
      return scope.regionCode() != null && scope.regionCode().equalsIgnoreCase(personnel.getRegionCode());
    }
    if (scope.adminUnit()) {
      return scope.unitCode() != null && scope.unitCode().equalsIgnoreCase(personnel.getUnitCode());
    }
    if (scope.userRole()) {
      return scope.currentPersonnelId() != null && scope.currentPersonnelId().equals(personnel.getId());
    }
    return false;
  }

  private boolean matchesKeyword(MilitaryPersonnel personnel, String keyword) {
    if (keyword == null) {
      return true;
    }
    String keywordLower = keyword.toLowerCase(Locale.ROOT);
    return containsIgnoreCase(personnel.getFullName(), keywordLower)
        || containsIgnoreCase(personnel.getCode(), keywordLower);
  }

  private boolean containsIgnoreCase(String value, String keywordLower) {
    return value != null && keywordLower != null && value.toLowerCase(Locale.ROOT).contains(keywordLower);
  }

  private record AccessScope(boolean systemAdmin, boolean adminRegion, boolean adminUnit, boolean userRole,
                             Long currentPersonnelId, String regionCode, String unitCode) {
  }

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
      throw new AppException(ErrorCode.PERSONNEL_IMAGE_SAVE_FAILED);
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

  private void deleteImageIfExists(String imagePath) {
    if (imagePath == null || imagePath.isEmpty()) {
      return;
    }
    try {
      s3Client.deleteObject(
          DeleteObjectRequest.builder()
              .bucket(bucketName)
              .key(buildObjectKey(imagePath))
              .build()
      );
    } catch (NoSuchKeyException ignored) {
      return;
    } catch (S3Exception ex) {
      if (ex.statusCode() == 404) {
        return;
      }
      throw new AppException(ErrorCode.PERSONNEL_IMAGE_DELETE_FAILED);
    }
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

  private MilitaryPersonnelResponse toResponse(MilitaryPersonnel personnel) {
    MilitaryPersonnelResponse response = new MilitaryPersonnelResponse(personnel);
    response.setImageUrl(personnel.getImagePath() == null ? null : IMAGE_ENDPOINT + personnel.getImagePath());
    return response;
  }
}
