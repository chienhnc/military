package com.military.service.impl;

import com.military.exception.AppException;
import com.military.exception.ErrorCode;
import com.military.models.MilitaryUnit;
import com.military.payload.request.MilitaryUnitRequest;
import com.military.payload.response.MilitaryUnitResponse;
import com.military.repository.MilitaryUnitRepository;
import com.military.service.MilitaryUnitService;
import com.military.service.dto.UnitLogo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

import java.io.IOException;
import java.util.UUID;

@Service
public class MilitaryUnitServiceImpl implements MilitaryUnitService {
  private static final String IMAGE_ENDPOINT = "/api/common/images/unit/";
  private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

  private final MilitaryUnitRepository militaryUnitRepository;
  private final S3Client s3Client;
  private final String bucketName;
  private final String objectPrefix;

  public MilitaryUnitServiceImpl(MilitaryUnitRepository militaryUnitRepository,
                                 S3Client s3Client,
                                 @Value("${military.app.s3.bucket}") String bucketName,
                                 @Value("${military.app.s3.unit-prefix:military-units}") String objectPrefix) {
    this.militaryUnitRepository = militaryUnitRepository;
    this.s3Client = s3Client;
    this.bucketName = bucketName;
    this.objectPrefix = normalizePrefix(objectPrefix);
    if (this.bucketName == null || this.bucketName.trim().isEmpty()) {
      throw new IllegalStateException("military.app.s3.bucket must not be empty");
    }
  }

  @Override
  public MilitaryUnitResponse create(MilitaryUnitRequest request) {
    if (militaryUnitRepository.existsByUnitCode(request.getUnitCode())) {
      throw new AppException(ErrorCode.MILITARY_UNIT_CODE_EXISTS);
    }
    MilitaryUnit saved = militaryUnitRepository.save(new MilitaryUnit(request));
    return toResponse(saved);
  }

  @Override
  public MilitaryUnitResponse update(Long id, MilitaryUnitRequest request) {
    MilitaryUnit unit = findEntityById(id);
    MilitaryUnit existingCodeOwner = militaryUnitRepository.findByUnitCodeIgnoreCase(request.getUnitCode())
        .orElse(null);
    if (existingCodeOwner != null && !existingCodeOwner.getId().equals(unit.getId())) {
      throw new AppException(ErrorCode.MILITARY_UNIT_CODE_EXISTS);
    }

    String oldLogoPath = unit.getLogoPath();
    unit.setRegionCode(request.getRegionCode());
    unit.setUnitCode(request.getUnitCode());
    unit.setUnitName(request.getUnitName());
    unit.setAddress(request.getAddress());
    unit.setEstablishedDate(request.getEstablishedDate());
    unit.setDescription(request.getDescription());
    unit.setLogoPath(request.getLogoPath());

    MilitaryUnit saved = militaryUnitRepository.save(unit);
    if (oldLogoPath != null && !oldLogoPath.equals(saved.getLogoPath())) {
      deleteLogoIfExists(oldLogoPath);
    }
    return toResponse(saved);
  }

  @Override
  public MilitaryUnitResponse getById(Long id) {
    return toResponse(findEntityById(id));
  }

  @Override
  public Page<MilitaryUnitResponse> list(String keyword, Pageable pageable) {
    if (keyword == null || keyword.trim().isEmpty()) {
      return militaryUnitRepository.findAll(pageable).map(this::toResponse);
    }
    String cleanKeyword = keyword.trim();
    return militaryUnitRepository
        .findByUnitCodeContainingIgnoreCaseOrUnitNameContainingIgnoreCase(cleanKeyword, cleanKeyword, pageable)
        .map(this::toResponse);
  }

  @Override
  public void delete(Long id) {
    MilitaryUnit unit = findEntityById(id);
    deleteLogoIfExists(unit.getLogoPath());
    militaryUnitRepository.delete(unit);
  }

  @Override
  public String storeLogo(MultipartFile imageFile) {
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
      throw new AppException(ErrorCode.MILITARY_UNIT_LOGO_SAVE_FAILED);
    }
  }

  @Override
  public UnitLogo loadLogo(String filename) {
    if (filename == null || filename.isBlank()) {
      throw new AppException(ErrorCode.MILITARY_UNIT_NOT_FOUND);
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
      return new UnitLogo(filename, contentType, objectBytes.asByteArray());
    } catch (NoSuchKeyException ex) {
      throw new AppException(ErrorCode.MILITARY_UNIT_NOT_FOUND);
    } catch (S3Exception ex) {
      if (ex.statusCode() == 404) {
        throw new AppException(ErrorCode.MILITARY_UNIT_NOT_FOUND);
      }
      throw new AppException(ErrorCode.MILITARY_UNIT_LOGO_SAVE_FAILED);
    }
  }

  private MilitaryUnit findEntityById(Long id) {
    return militaryUnitRepository.findById(id)
        .orElseThrow(() -> new AppException(ErrorCode.MILITARY_UNIT_NOT_FOUND));
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

  private void deleteLogoIfExists(String logoPath) {
    if (logoPath == null || logoPath.isBlank()) {
      return;
    }
    try {
      s3Client.deleteObject(
          DeleteObjectRequest.builder()
              .bucket(bucketName)
              .key(buildObjectKey(logoPath))
              .build()
      );
    } catch (NoSuchKeyException ignored) {
      return;
    } catch (S3Exception ex) {
      if (ex.statusCode() == 404) {
        return;
      }
      throw new AppException(ErrorCode.MILITARY_UNIT_LOGO_DELETE_FAILED);
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

  private MilitaryUnitResponse toResponse(MilitaryUnit unit) {
    MilitaryUnitResponse response = new MilitaryUnitResponse(unit);
    response.setLogoUrl(unit.getLogoPath() == null ? null : IMAGE_ENDPOINT + unit.getLogoPath());
    return response;
  }
}
