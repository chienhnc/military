package com.military.service;

import com.military.payload.request.LeaveRequestActionRequest;
import com.military.payload.request.LeaveRequestCreateRequest;
import com.military.payload.request.LeaveRequestUpdateRequest;
import com.military.payload.response.LeaveApprovalCapabilityResponse;
import com.military.payload.response.LeaveRequestHistoryResponse;
import com.military.payload.response.LeaveRequestResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface LeaveRequestService {
  LeaveRequestResponse create(LeaveRequestCreateRequest request);

  LeaveRequestResponse getById(Long id);

  Page<LeaveRequestResponse> listMine(Pageable pageable);

  Page<LeaveRequestResponse> listPendingApproval(Pageable pageable);

  List<LeaveRequestHistoryResponse> getHistories(Long id);

  LeaveRequestResponse accept(Long id, LeaveRequestActionRequest request);

  LeaveRequestResponse approve(Long id, LeaveRequestActionRequest request);

  LeaveRequestResponse sendBack(Long id, LeaveRequestActionRequest request);

  LeaveRequestResponse updateForResubmit(Long id, LeaveRequestUpdateRequest request);

  LeaveRequestResponse resubmit(Long id, LeaveRequestActionRequest request);

  LeaveRequestResponse openSupplement(Long id, LeaveRequestActionRequest request);

  LeaveRequestResponse submitNext(Long id, LeaveRequestActionRequest request);

  LeaveApprovalCapabilityResponse checkApprovalCapability(Long leaveRequestId);
}
