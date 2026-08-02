package com.military.service.impl;

import com.military.models.ELeaveRequestStatus;
import com.military.models.LeaveRequest;
import com.military.models.MilitaryPersonnel;
import com.military.models.User;
import com.military.payload.response.LeaveRequestResponse;
import com.military.repository.LeaveApprovalConfigRepository;
import com.military.repository.LeaveRequestHistoryRepository;
import com.military.repository.LeaveRequestRepository;
import com.military.repository.MilitaryPersonnelRepository;
import com.military.repository.SubmissionFlowRepository;
import com.military.repository.SubmissionGroupRepository;
import com.military.repository.UserRepository;
import com.military.security.services.UserDetailsImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeaveRequestServiceImpl.listPendingApproval() personnel enrichment")
class LeaveRequestServiceImplPersonnelEnrichmentTest {

  @Mock
  private LeaveRequestRepository leaveRequestRepository;

  @Mock
  private LeaveRequestHistoryRepository leaveRequestHistoryRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private MilitaryPersonnelRepository militaryPersonnelRepository;

  @Mock
  private SubmissionFlowRepository submissionFlowRepository;

  @Mock
  private SubmissionGroupRepository submissionGroupRepository;

  @Mock
  private LeaveApprovalConfigRepository leaveApprovalConfigRepository;

  private LeaveRequestServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new LeaveRequestServiceImpl(
        leaveRequestRepository,
        leaveRequestHistoryRepository,
        userRepository,
        militaryPersonnelRepository,
        submissionFlowRepository,
        submissionGroupRepository,
        leaveApprovalConfigRepository
    );

    User approver = new User();
    approver.setId(1L);
    approver.setUsername("approver");
    approver.setMilitaryPersonnelId(900L);

    MilitaryPersonnel approverPersonnel = new MilitaryPersonnel();
    approverPersonnel.setId(900L);
    approverPersonnel.setFullName("Approver");

    when(userRepository.findById(1L)).thenReturn(Optional.of(approver));
    when(militaryPersonnelRepository.findById(900L)).thenReturn(Optional.of(approverPersonnel));

    List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN_UNIT"));
    UserDetailsImpl principal = new UserDetailsImpl(1L, "approver", "approver@example.com", "password", authorities);
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, null, authorities));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("Should embed requester's militaryPersonnel DTO in each pending item")
  void testPendingListIncludesMilitaryPersonnelDto() {
    LeaveRequest leaveRequest = new LeaveRequest();
    leaveRequest.setId(10L);
    leaveRequest.setMilitaryPersonnelId(100L);
    leaveRequest.setCurrentAssignee("approver");
    leaveRequest.setStatus(ELeaveRequestStatus.CHUA_XU_LY);

    MilitaryPersonnel requesterPersonnel = new MilitaryPersonnel();
    requesterPersonnel.setId(100L);
    requesterPersonnel.setFullName("John Doe");
    requesterPersonnel.setUnitCode("UNIT-A");
    requesterPersonnel.setImagePath("avatar.jpg");

    when(leaveRequestRepository.findAllList()).thenReturn(List.of(leaveRequest));
    when(militaryPersonnelRepository.findById(100L)).thenReturn(Optional.of(requesterPersonnel));

    Page<LeaveRequestResponse> result = service.listPendingApproval(PageRequest.of(0, 10));

    assertEquals(1, result.getTotalElements());
    LeaveRequestResponse response = result.getContent().get(0);
    assertEquals("John Doe", response.getMilitaryPersonnel().getFullName());
    assertEquals("UNIT-A", response.getMilitaryPersonnel().getUnitCode());
    assertEquals("/api/common/images/personnel/avatar.jpg", response.getMilitaryPersonnel().getImageUrl());
  }

  @Test
  @DisplayName("Should leave militaryPersonnel null when the linked personnel no longer exists")
  void testPendingListWithMissingPersonnelLeavesDtoNull() {
    LeaveRequest leaveRequest = new LeaveRequest();
    leaveRequest.setId(11L);
    leaveRequest.setMilitaryPersonnelId(999L);
    leaveRequest.setCurrentAssignee("approver");
    leaveRequest.setStatus(ELeaveRequestStatus.CHUA_XU_LY);

    when(leaveRequestRepository.findAllList()).thenReturn(List.of(leaveRequest));
    when(militaryPersonnelRepository.findById(999L)).thenReturn(Optional.empty());

    Page<LeaveRequestResponse> result = service.listPendingApproval(PageRequest.of(0, 10));

    assertEquals(1, result.getTotalElements());
    assertNull(result.getContent().get(0).getMilitaryPersonnel());
  }
}
