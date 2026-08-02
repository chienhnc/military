package com.military.service.impl;

import com.google.gson.Gson;
import com.military.exception.AppException;
import com.military.exception.ErrorCode;
import com.military.models.ELeaveRequestStatus;
import com.military.models.EQrScanStatus;
import com.military.models.EQrScanType;
import com.military.models.LeaveRequest;
import com.military.models.LeaveRequestHistory;
import com.military.models.MilitaryPersonnel;
import com.military.models.QrScanLog;
import com.military.models.Vehicle;
import com.military.payload.request.QrCitizenDataRequest;
import com.military.payload.request.QrMilitaryPersonnelDataRequest;
import com.military.payload.request.QrScanDecisionRequest;
import com.military.payload.request.QrScanRequest;
import com.military.payload.response.QrScanLogResponse;
import com.military.payload.response.VehicleResponse;
import com.military.repository.LeaveRequestHistoryRepository;
import com.military.repository.LeaveRequestRepository;
import com.military.repository.MilitaryPersonnelRepository;
import com.military.repository.QrScanLogRepository;
import com.military.repository.VehicleRepository;
import com.military.service.QrScanLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class QrScanLogServiceImpl implements QrScanLogService {
  private static final String IMAGE_ENDPOINT = "/api/common/images/personnel/";
  private static final String VEHICLE_IMAGE_ENDPOINT = "/api/common/images/vehicle/";

  private final QrScanLogRepository qrScanLogRepository;
  private final MilitaryPersonnelRepository militaryPersonnelRepository;
  private final LeaveRequestRepository leaveRequestRepository;
  private final LeaveRequestHistoryRepository leaveRequestHistoryRepository;
  private final VehicleRepository vehicleRepository;
  private final Gson gson;

  public QrScanLogServiceImpl(QrScanLogRepository qrScanLogRepository,
                              MilitaryPersonnelRepository militaryPersonnelRepository,
                              LeaveRequestRepository leaveRequestRepository,
                              LeaveRequestHistoryRepository leaveRequestHistoryRepository,
                              VehicleRepository vehicleRepository,
                              Gson gson) {
    this.qrScanLogRepository = qrScanLogRepository;
    this.militaryPersonnelRepository = militaryPersonnelRepository;
    this.leaveRequestRepository = leaveRequestRepository;
    this.leaveRequestHistoryRepository = leaveRequestHistoryRepository;
    this.vehicleRepository = vehicleRepository;
    this.gson = gson;
  }

  @Override
  public QrScanLogResponse scan(QrScanRequest request) {
    validateScanPayload(request);
    if (request.getCitizen() != null) {
      return scanCitizen(request.getCitizen());
    }
    return scanMilitary(request.getMilitaryPersonnel());
  }

  @Override
  public QrScanLogResponse approveCitizen(Long id, QrScanDecisionRequest request) {
    QrScanLog log = findLogById(id);
    validateCitizenDecision(log);
    log.setStatus(EQrScanStatus.DONG_Y);
    log.setReason(request == null ? null : request.getReason());
    return toResponse(qrScanLogRepository.save(log));
  }

  @Override
  public QrScanLogResponse rejectCitizen(Long id, QrScanDecisionRequest request) {
    QrScanLog log = findLogById(id);
    validateCitizenDecision(log);
    log.setStatus(EQrScanStatus.TU_CHOI);
    log.setReason(request == null ? "Tu choi." : request.getReason());
    return toResponse(qrScanLogRepository.save(log));
  }

  @Override
  public QrScanLogResponse getById(Long id) {
    return toResponse(findLogById(id));
  }

  @Override
  public Page<QrScanLogResponse> list(EQrScanType scanType, EQrScanStatus status, Pageable pageable) {
    List<QrScanLogResponse> data = qrScanLogRepository.findAllList().stream()
        .filter(item -> scanType == null || scanType == item.getScanType())
        .filter(item -> status == null || status == item.getStatus())
        .sorted(Comparator.comparing(QrScanLog::getScannedAt, Comparator.nullsLast(Comparator.reverseOrder())))
        .map(this::toResponse)
        .toList();
    return paginate(data, pageable);
  }

  private Page<QrScanLogResponse> paginate(List<QrScanLogResponse> data, Pageable pageable) {
    int start = (int) pageable.getOffset();
    if (start >= data.size()) {
      return new PageImpl<>(List.of(), pageable, data.size());
    }
    int end = Math.min(start + pageable.getPageSize(), data.size());
    return new PageImpl<>(data.subList(start, end), pageable, data.size());
  }

  private QrScanLogResponse scanCitizen(QrCitizenDataRequest citizen) {
    QrScanLog log = new QrScanLog();
    log.setScanType(EQrScanType.CITIZEN);
    log.setScannedAt(nowIso());
    log.setStatus(EQrScanStatus.DANG_XU_LY);
    log.setCitizenName(citizen.getName());
    log.setCitizenBirthday(citizen.getBirthday());
    log.setCitizenAddress(citizen.getAddress());
    log.setCitizenId(citizen.getCitizenId());
    log.setCitizenIssueDate(citizen.getIssueDate());
    log.setPayloadJson(gson.toJson(citizen));
    return toResponse(qrScanLogRepository.save(log));
  }

  private QrScanLogResponse scanMilitary(QrMilitaryPersonnelDataRequest military) {
    MilitaryPersonnel personnel = resolvePersonnel(military);
    LocalDate today = LocalDate.now();
    QrScanLog log = new QrScanLog();
    log.setScanType(EQrScanType.MILITARY);
    log.setScannedAt(nowIso());
    log.setMilitaryPersonnelId(personnel.getId());
    log.setMilitaryPersonnelCode(personnel.getCode());
    log.setMilitaryPersonnelFullName(personnel.getFullName());
    log.setPayloadJson(gson.toJson(military));

    Optional<ApprovedRequestCandidate> approvedRequest = findBestApprovedRequest(personnel.getId(), today);
    if (approvedRequest.isEmpty()) {
      log.setStatus(EQrScanStatus.TU_CHOI);
      log.setReason("Khong co quyen ra.");
      return toResponse(qrScanLogRepository.save(log));
    }

    LeaveRequest selected = approvedRequest.get().leaveRequest();
    Integer allowed = selected.getAllowedOutCount();
    Integer used = selected.getUsedOutCount() == null ? 0 : selected.getUsedOutCount();
    if (allowed == null || used >= allowed) {
      log.setStatus(EQrScanStatus.TU_CHOI);
      log.setReason("Khong co quyen ra.");
      log.setLeaveRequestId(selected.getId());
      log.setApprovedRoundNo(approvedRequest.get().approvedRoundNo());
      return toResponse(qrScanLogRepository.save(log));
    }

    selected.setUsedOutCount(used + 1);
    leaveRequestRepository.save(selected);

    log.setStatus(EQrScanStatus.DONG_Y);
    log.setReason("Dong y.");
    log.setLeaveRequestId(selected.getId());
    log.setApprovedRoundNo(approvedRequest.get().approvedRoundNo());
    return toResponse(qrScanLogRepository.save(log));
  }

  private Optional<ApprovedRequestCandidate> findBestApprovedRequest(Long militaryPersonnelId, LocalDate today) {
    List<LeaveRequest> candidates = leaveRequestRepository.findAllList().stream()
        .filter(item -> militaryPersonnelId.equals(item.getMilitaryPersonnelId()))
        .filter(item -> item.getStatus() == ELeaveRequestStatus.DA_DUYET)
        .filter(item -> item.getLeaveFrom() != null && item.getLeaveTo() != null)
        .filter(item -> !today.isBefore(item.getLeaveFrom()) && !today.isAfter(item.getLeaveTo()))
        .toList();

    return candidates.stream()
        .map(item -> {
          Optional<String> latestApprovedRound = leaveRequestHistoryRepository.findAllByLeaveRequestId(item.getId()).stream()
              .filter(history -> history.getStatus() == ELeaveRequestStatus.DA_DUYET)
              .map(LeaveRequestHistory::getRoundNo)
              .max(this::compareRoundDesc);
          if (latestApprovedRound.isEmpty()) {
            return null;
          }
          return new ApprovedRequestCandidate(item, latestApprovedRound.get());
        })
        .filter(java.util.Objects::nonNull)
        .sorted((left, right) -> compareRoundDesc(right.approvedRoundNo(), left.approvedRoundNo()))
        .findFirst();
  }

  private int compareRoundDesc(String left, String right) {
    Round roundLeft = parseRound(left);
    Round roundRight = parseRound(right);
    if (roundLeft.major() != roundRight.major()) {
      return Integer.compare(roundLeft.major(), roundRight.major());
    }
    return Integer.compare(roundLeft.sequence(), roundRight.sequence());
  }

  private Round parseRound(String roundNo) {
    if (roundNo == null || !roundNo.contains(".")) {
      return new Round(0, 0);
    }
    String[] parts = roundNo.split("\\.");
    if (parts.length != 2) {
      return new Round(0, 0);
    }
    try {
      return new Round(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    } catch (NumberFormatException ex) {
      return new Round(0, 0);
    }
  }

  private void validateScanPayload(QrScanRequest request) {
    if (request == null) {
      throw new AppException(ErrorCode.QR_SCAN_INVALID_PAYLOAD);
    }
    boolean hasMilitary = request.getMilitaryPersonnel() != null;
    boolean hasCitizen = request.getCitizen() != null;
    if (hasMilitary == hasCitizen) {
      throw new AppException(ErrorCode.QR_SCAN_INVALID_PAYLOAD);
    }
  }

  private MilitaryPersonnel resolvePersonnel(QrMilitaryPersonnelDataRequest military) {
    if (military == null) {
      throw new AppException(ErrorCode.QR_SCAN_INVALID_PAYLOAD);
    }
    if (military.getId() != null) {
      return militaryPersonnelRepository.findById(military.getId())
          .orElseThrow(() -> new AppException(ErrorCode.PERSONNEL_NOT_FOUND));
    }
    if (military.getCode() != null && !military.getCode().isBlank()) {
      return militaryPersonnelRepository.findAllList().stream()
          .filter(item -> item.getCode() != null && item.getCode().equalsIgnoreCase(military.getCode()))
          .findFirst()
          .orElseThrow(() -> new AppException(ErrorCode.PERSONNEL_NOT_FOUND));
    }
    throw new AppException(ErrorCode.QR_SCAN_INVALID_PAYLOAD);
  }

  private QrScanLog findLogById(Long id) {
    return qrScanLogRepository.findById(id)
        .orElseThrow(() -> new AppException(ErrorCode.QR_SCAN_LOG_NOT_FOUND));
  }

  private void validateCitizenDecision(QrScanLog log) {
    if (log.getScanType() != EQrScanType.CITIZEN) {
      throw new AppException(ErrorCode.QR_SCAN_CITIZEN_ACTION_ONLY);
    }
    if (log.getStatus() != EQrScanStatus.DANG_XU_LY) {
      throw new AppException(ErrorCode.QR_SCAN_INVALID_STATE);
    }
  }

  private String nowIso() {
    return Instant.now().toString();
  }

  private QrScanLogResponse toResponse(QrScanLog log) {
    QrScanLogResponse response = new QrScanLogResponse(log);
    if (log.getScanType() == EQrScanType.MILITARY && log.getMilitaryPersonnelId() != null) {
      militaryPersonnelRepository.findById(log.getMilitaryPersonnelId()).ifPresent(personnel -> {
        response.setMilitaryPersonnelImageUrl(
            personnel.getImagePath() == null ? null : IMAGE_ENDPOINT + personnel.getImagePath());
        vehicleRepository.findByPersonnelId(personnel.getId())
            .ifPresent(vehicle -> response.setMilitaryPersonnelVehicle(toVehicleResponse(vehicle)));
      });
    }
    return response;
  }

  private VehicleResponse toVehicleResponse(Vehicle vehicle) {
    VehicleResponse response = new VehicleResponse(vehicle);
    response.setImageUrls(vehicle.getImagePaths() == null ? List.of()
        : vehicle.getImagePaths().stream().map(p -> VEHICLE_IMAGE_ENDPOINT + p).toList());
    return response;
  }

  private record ApprovedRequestCandidate(LeaveRequest leaveRequest, String approvedRoundNo) {
  }

  private record Round(int major, int sequence) {
  }
}
