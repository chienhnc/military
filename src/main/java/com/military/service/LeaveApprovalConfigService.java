package com.military.service;

import com.military.models.EMilitaryPosition;
import com.military.payload.request.LeaveApprovalConfigRequest;
import com.military.payload.response.LeaveApprovalConfigResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface LeaveApprovalConfigService {
  LeaveApprovalConfigResponse create(LeaveApprovalConfigRequest request);

  LeaveApprovalConfigResponse update(Long id, LeaveApprovalConfigRequest request);

  LeaveApprovalConfigResponse updateActive(Long id, Boolean active);

  LeaveApprovalConfigResponse getById(Long id);

  Page<LeaveApprovalConfigResponse> list(EMilitaryPosition militaryPosition,
                                         Boolean active,
                                         LocalDate effectiveFrom,
                                         LocalDate effectiveTo,
                                         Pageable pageable);

  LeaveApprovalConfigResponse getApplicable(EMilitaryPosition militaryPosition, LocalDate applyDate);

  void delete(Long id);
}
