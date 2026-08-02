package com.military.service.impl;

import com.google.gson.Gson;
import com.military.models.EQrScanStatus;
import com.military.models.EQrScanType;
import com.military.models.MilitaryPersonnel;
import com.military.models.QrScanLog;
import com.military.models.Vehicle;
import com.military.payload.request.QrMilitaryPersonnelDataRequest;
import com.military.payload.request.QrScanRequest;
import com.military.payload.response.QrScanLogResponse;
import com.military.repository.LeaveRequestHistoryRepository;
import com.military.repository.LeaveRequestRepository;
import com.military.repository.MilitaryPersonnelRepository;
import com.military.repository.QrScanLogRepository;
import com.military.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("QrScanLogServiceImpl.scan() personnel avatar/vehicle enrichment")
class QrScanLogServiceImplTest {

  @Mock
  private QrScanLogRepository qrScanLogRepository;

  @Mock
  private MilitaryPersonnelRepository militaryPersonnelRepository;

  @Mock
  private LeaveRequestRepository leaveRequestRepository;

  @Mock
  private LeaveRequestHistoryRepository leaveRequestHistoryRepository;

  @Mock
  private VehicleRepository vehicleRepository;

  private QrScanLogServiceImpl qrScanLogService;

  @BeforeEach
  void setUp() {
    qrScanLogService = new QrScanLogServiceImpl(
        qrScanLogRepository,
        militaryPersonnelRepository,
        leaveRequestRepository,
        leaveRequestHistoryRepository,
        vehicleRepository,
        new Gson()
    );
  }

  private void stubSaveReturnsArgument() {
    when(qrScanLogRepository.save(org.mockito.ArgumentMatchers.any(QrScanLog.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  @DisplayName("Should include personnel imageUrl and vehicle when scanning a military QR")
  void testScanMilitaryIncludesAvatarAndVehicle() {
    MilitaryPersonnel personnel = new MilitaryPersonnel();
    personnel.setId(100L);
    personnel.setCode("UNIT|RANK|POS|00001");
    personnel.setFullName("John Doe");
    personnel.setImagePath("avatar.jpg");

    Vehicle vehicle = new Vehicle();
    vehicle.setId(200L);
    vehicle.setPersonnelId(100L);
    vehicle.setLicensePlate("29A-123.45");
    vehicle.setImagePaths(List.of("vehicle1.jpg"));

    stubSaveReturnsArgument();
    when(militaryPersonnelRepository.findById(100L)).thenReturn(Optional.of(personnel));
    when(leaveRequestRepository.findAllList()).thenReturn(List.of());
    when(vehicleRepository.findByPersonnelId(100L)).thenReturn(Optional.of(vehicle));

    QrMilitaryPersonnelDataRequest militaryData = new QrMilitaryPersonnelDataRequest();
    militaryData.setId(100L);
    QrScanRequest request = new QrScanRequest();
    request.setMilitaryPersonnel(militaryData);

    QrScanLogResponse response = qrScanLogService.scan(request);

    assertEquals("/api/common/images/personnel/avatar.jpg", response.getMilitaryPersonnelImageUrl());
    assertEquals("29A-123.45", response.getMilitaryPersonnelVehicle().getLicensePlate());
    assertEquals(List.of("/api/common/images/vehicle/vehicle1.jpg"),
        response.getMilitaryPersonnelVehicle().getImageUrls());
  }

  @Test
  @DisplayName("Should leave avatar/vehicle null when personnel has no image or vehicle")
  void testScanMilitaryWithoutAvatarOrVehicle() {
    MilitaryPersonnel personnel = new MilitaryPersonnel();
    personnel.setId(100L);
    personnel.setCode("UNIT|RANK|POS|00001");
    personnel.setFullName("John Doe");

    stubSaveReturnsArgument();
    when(militaryPersonnelRepository.findById(100L)).thenReturn(Optional.of(personnel));
    when(leaveRequestRepository.findAllList()).thenReturn(List.of());
    when(vehicleRepository.findByPersonnelId(100L)).thenReturn(Optional.empty());

    QrMilitaryPersonnelDataRequest militaryData = new QrMilitaryPersonnelDataRequest();
    militaryData.setId(100L);
    QrScanRequest request = new QrScanRequest();
    request.setMilitaryPersonnel(militaryData);

    QrScanLogResponse response = qrScanLogService.scan(request);

    assertNull(response.getMilitaryPersonnelImageUrl());
    assertNull(response.getMilitaryPersonnelVehicle());
  }

  @Test
  @DisplayName("list() should filter by scanType and status and sort most recent scan first")
  void testListFiltersAndSortsByMostRecent() {
    QrScanLog older = new QrScanLog();
    older.setId(1L);
    older.setScanType(EQrScanType.MILITARY);
    older.setScannedAt("2026-08-01T00:00:00Z");
    older.setStatus(EQrScanStatus.DONG_Y);
    older.setMilitaryPersonnelId(100L);

    QrScanLog newer = new QrScanLog();
    newer.setId(2L);
    newer.setScanType(EQrScanType.MILITARY);
    newer.setScannedAt("2026-08-02T00:00:00Z");
    newer.setStatus(EQrScanStatus.DONG_Y);
    newer.setMilitaryPersonnelId(100L);

    QrScanLog citizenLog = new QrScanLog();
    citizenLog.setId(3L);
    citizenLog.setScanType(EQrScanType.CITIZEN);
    citizenLog.setScannedAt("2026-08-03T00:00:00Z");
    citizenLog.setStatus(EQrScanStatus.DANG_XU_LY);

    when(qrScanLogRepository.findAllList()).thenReturn(List.of(older, newer, citizenLog));
    when(militaryPersonnelRepository.findById(100L)).thenReturn(Optional.empty());

    Page<QrScanLogResponse> result = qrScanLogService.list(EQrScanType.MILITARY, EQrScanStatus.DONG_Y,
        PageRequest.of(0, 10));

    assertEquals(2, result.getTotalElements());
    assertEquals(List.of(2L, 1L), result.getContent().stream().map(QrScanLogResponse::getId).toList());
  }

  @Test
  @DisplayName("list() without filters should return every log, paginated")
  void testListWithoutFiltersReturnsAll() {
    QrScanLog military = new QrScanLog();
    military.setId(1L);
    military.setScanType(EQrScanType.MILITARY);
    military.setScannedAt("2026-08-01T00:00:00Z");
    military.setStatus(EQrScanStatus.DONG_Y);
    military.setMilitaryPersonnelId(100L);

    QrScanLog citizenLog = new QrScanLog();
    citizenLog.setId(2L);
    citizenLog.setScanType(EQrScanType.CITIZEN);
    citizenLog.setScannedAt("2026-08-02T00:00:00Z");
    citizenLog.setStatus(EQrScanStatus.DANG_XU_LY);

    when(qrScanLogRepository.findAllList()).thenReturn(List.of(military, citizenLog));
    when(militaryPersonnelRepository.findById(100L)).thenReturn(Optional.empty());

    Page<QrScanLogResponse> result = qrScanLogService.list(null, null, PageRequest.of(0, 10));

    assertEquals(2, result.getTotalElements());
  }
}
