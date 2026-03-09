package com.military.service.impl;

import com.google.gson.Gson;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.military.exception.AppException;
import com.military.exception.ErrorCode;
import com.military.models.MilitaryPersonnel;
import com.military.payload.request.MilitaryPersonnelRequest;
import com.military.payload.response.MilitaryPersonnelResponse;
import com.military.repository.MilitaryPersonnelRepository;
import com.military.service.MilitaryPersonnelService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class MilitaryPersonnelServiceImpl implements MilitaryPersonnelService {
  private static final Pattern NON_ASCII = Pattern.compile("[^\\p{ASCII}]");
  private static final Pattern NON_ALNUM = Pattern.compile("[^A-Za-z0-9]+");
  private static final Pattern MULTI_DASH = Pattern.compile("-+");
  private static final int QR_SIZE = 300;
  private static final String IMAGE_ENDPOINT = "/api/personnel/images/";

  private final MilitaryPersonnelRepository militaryPersonnelRepository;
  private final Path uploadDirectory;
  private final Gson gson;

  public MilitaryPersonnelServiceImpl(MilitaryPersonnelRepository militaryPersonnelRepository,
                                      @Value("${military.app.upload-dir:uploads/personnel}") String uploadDir, Gson gson) {
    this.militaryPersonnelRepository = militaryPersonnelRepository;
    this.uploadDirectory = Paths.get(uploadDir).toAbsolutePath().normalize();
    this.gson = gson;
    try {
      Files.createDirectories(this.uploadDirectory);
    } catch (IOException ex) {
      throw new AppException(ErrorCode.PERSONNEL_IMAGE_SAVE_FAILED);
    }
  }

  @Transactional
  public MilitaryPersonnelResponse create(MilitaryPersonnelRequest militaryPersonnelRequest) {
    MilitaryPersonnel personnel = new MilitaryPersonnel(militaryPersonnelRequest);
    personnel.setCode(generateCode(militaryPersonnelRequest));
    personnel.setQrCode(generateQrCode(personnel));

    MilitaryPersonnel saved = militaryPersonnelRepository.save(personnel);
    return toResponse(saved);
  }

  @Transactional
  public MilitaryPersonnelResponse update(Long id,
                                          MilitaryPersonnelRequest militaryPersonnelRequest) {
    MilitaryPersonnel personnel = findEntityById(id);
    personnel.setFullName(militaryPersonnelRequest.getFullName());

    boolean shouldRegenerateCode = isPrefixChanged(personnel, militaryPersonnelRequest.getUnitCode(),
            militaryPersonnelRequest.getRankCode(), militaryPersonnelRequest.getPositionCode());
    personnel.setRankCode(militaryPersonnelRequest.getRankCode());
    personnel.setUnitCode(militaryPersonnelRequest.getUnitCode());
    personnel.setPositionCode(militaryPersonnelRequest.getPositionCode());

    if (shouldRegenerateCode) {
      personnel.setCode(generateCode(militaryPersonnelRequest));
    }
    personnel.setImagePath(militaryPersonnelRequest.getImagePath());
    personnel.setQrCode(generateQrCode(personnel));
    MilitaryPersonnel saved = militaryPersonnelRepository.save(personnel);
    return toResponse(saved);
  }

  @Transactional(readOnly = true)
  public MilitaryPersonnelResponse getById(Long id) {
    return toResponse(findEntityById(id));
  }

  @Transactional(readOnly = true)
  public Page<MilitaryPersonnelResponse> list(String keyword, Pageable pageable) {
    if (keyword == null || keyword.trim().isEmpty()) {
      return militaryPersonnelRepository.findAll(pageable).map(this::toResponse);
    }
    String cleanKeyword = keyword.trim();
    return militaryPersonnelRepository
        .findByFullNameContainingIgnoreCaseOrCodeContainingIgnoreCase(cleanKeyword, cleanKeyword, pageable)
        .map(this::toResponse);
  }

  @Transactional
  public void delete(Long id) {
    MilitaryPersonnel personnel = findEntityById(id);
    deleteImageIfExists(personnel.getImagePath());
    militaryPersonnelRepository.delete(personnel);
  }

  @Transactional(readOnly = true)
  public Path resolveImagePath(String filename) {
    Path path = uploadDirectory.resolve(filename).normalize();
    if (!path.startsWith(uploadDirectory)) {
      throw new AppException(ErrorCode.PERSONNEL_NOT_FOUND);
    }
    if (!Files.exists(path)) {
      throw new AppException(ErrorCode.PERSONNEL_NOT_FOUND);
    }
    return path;
  }

  private MilitaryPersonnel findEntityById(Long id) {
    return militaryPersonnelRepository.findById(id)
        .orElseThrow(() -> new AppException(ErrorCode.PERSONNEL_NOT_FOUND));
  }

  private boolean isPrefixChanged(MilitaryPersonnel personnel, String unitName, String rank, String position) {
    String currentPrefix = buildPrefix(personnel.getUnitCode(), personnel.getRankCode(), personnel.getPositionCode());
    String newPrefix = buildPrefix(unitName, rank, position);
    return !currentPrefix.equals(newPrefix);
  }

  private synchronized String generateCode(MilitaryPersonnelRequest militaryPersonnelRequest) {
    String prefix = buildPrefix(militaryPersonnelRequest.getUnitCode(), militaryPersonnelRequest.getRankCode(), militaryPersonnelRequest.getPositionCode());
    String searchPrefix = prefix + "-";
    Optional<MilitaryPersonnel> latest = militaryPersonnelRepository
        .findFirstByCodeStartingWithOrderByCodeDesc(searchPrefix);

    int nextNumber = 1;
    if (latest.isPresent()) {
      String latestCode = latest.get().getCode();
      int index = latestCode.lastIndexOf("-");
      if (index >= 0 && index + 1 < latestCode.length()) {
        String sequenceStr = latestCode.substring(index + 1);
        try {
          nextNumber = Integer.parseInt(sequenceStr) + 1;
        } catch (NumberFormatException ignored) {
          nextNumber = 1;
        }
      }
    }

    String code;
    do {
      code = prefix + "-" + String.format("%05d", nextNumber++);
    } while (militaryPersonnelRepository.existsByCode(code));

    return code;
  }

  private String buildPrefix(String unitCode, String rank, String position) {
    String unitSegment = sanitizeForCode(unitCode);
    String rankSegment = sanitizeForCode(rank);
    String positionSegment = sanitizeForCode(position);
    return unitSegment + "-" + rankSegment + "-" + positionSegment;
  }

  private String sanitizeForCode(String value) {
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

  public String storeImage(MultipartFile imageFile) {
    if (imageFile == null || imageFile.isEmpty()) {
      return null;
    }

    String extension = extractExtension(imageFile.getOriginalFilename());
    String fileName = UUID.randomUUID() + extension;
    Path target = uploadDirectory.resolve(fileName).normalize();

    try {
      Files.copy(imageFile.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
      return fileName;
    } catch (IOException ex) {
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
    Path path = uploadDirectory.resolve(imagePath).normalize();
    try {
      Files.deleteIfExists(path);
    } catch (IOException ex) {
      throw new AppException(ErrorCode.PERSONNEL_IMAGE_DELETE_FAILED);
    }
  }

  private MilitaryPersonnelResponse toResponse(MilitaryPersonnel personnel) {
    MilitaryPersonnelResponse response = new MilitaryPersonnelResponse(personnel);
    response.setImageUrl(personnel.getImagePath() == null ? null : IMAGE_ENDPOINT + personnel.getImagePath());
    return response;
  }
}
