package com.military.service.impl;

import com.military.exception.AppException;
import com.military.exception.ErrorCode;
import com.military.models.EMilitaryPosition;
import com.military.models.LeaveApprovalConfig;
import com.military.payload.request.LeaveApprovalConfigRequest;
import com.military.payload.response.LeaveApprovalConfigResponse;
import com.military.repository.LeaveApprovalConfigRepository;
import com.military.service.LeaveApprovalConfigService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class LeaveApprovalConfigServiceImpl implements LeaveApprovalConfigService {
  private final LeaveApprovalConfigRepository leaveApprovalConfigRepository;

  public LeaveApprovalConfigServiceImpl(LeaveApprovalConfigRepository leaveApprovalConfigRepository) {
    this.leaveApprovalConfigRepository = leaveApprovalConfigRepository;
  }

  @Override
  public LeaveApprovalConfigResponse create(LeaveApprovalConfigRequest request) {
    validateRequest(request);
    validateUnique(request, null);
    validateNoOverlap(request, null);
    LeaveApprovalConfig config = new LeaveApprovalConfig(request);
    return toResponse(leaveApprovalConfigRepository.save(config));
  }

  @Override
  public LeaveApprovalConfigResponse update(Long id, LeaveApprovalConfigRequest request) {
    validateRequest(request);
    validateUnique(request, id);
    validateNoOverlap(request, id);
    LeaveApprovalConfig config = findEntityById(id);
    config.setMilitaryPosition(request.getMilitaryPosition());
    config.setMaxApprovalDays(request.getMaxApprovalDays());
    config.setEffectiveFrom(request.getEffectiveFrom());
    config.setEffectiveTo(request.getEffectiveTo());
    config.setActive(request.getActive());
    return toResponse(leaveApprovalConfigRepository.save(config));
  }

  @Override
  public LeaveApprovalConfigResponse updateActive(Long id, Boolean active) {
    LeaveApprovalConfig config = findEntityById(id);
    if (active == null) {
      throw new AppException(ErrorCode.LEAVE_APPROVAL_CONFIG_INVALID_ACTIVE);
    }
    config.setActive(active);
    return toResponse(leaveApprovalConfigRepository.save(config));
  }

  @Override
  public LeaveApprovalConfigResponse getById(Long id) {
    return toResponse(findEntityById(id));
  }

  @Override
  public Page<LeaveApprovalConfigResponse> list(EMilitaryPosition militaryPosition,
                                                Boolean active,
                                                LocalDate effectiveFrom,
                                                LocalDate effectiveTo,
                                                Pageable pageable) {
    return leaveApprovalConfigRepository.findByFilters(
        militaryPosition, active, effectiveFrom, effectiveTo, pageable).map(this::toResponse);
  }

  @Override
  public LeaveApprovalConfigResponse getApplicable(EMilitaryPosition militaryPosition, LocalDate applyDate) {
    if (militaryPosition == null || applyDate == null) {
      throw new AppException(ErrorCode.LEAVE_APPROVAL_CONFIG_INVALID_APPLICABLE_QUERY);
    }
    LeaveApprovalConfig config = leaveApprovalConfigRepository.findApplicable(militaryPosition, applyDate)
        .orElseThrow(() -> new AppException(ErrorCode.LEAVE_APPROVAL_CONFIG_NOT_FOUND));
    return toResponse(config);
  }

  @Override
  public void delete(Long id) {
    leaveApprovalConfigRepository.delete(findEntityById(id));
  }

  private LeaveApprovalConfig findEntityById(Long id) {
    return leaveApprovalConfigRepository.findById(id)
        .orElseThrow(() -> new AppException(ErrorCode.LEAVE_APPROVAL_CONFIG_NOT_FOUND));
  }

  private void validateRequest(LeaveApprovalConfigRequest request) {
    if (request.getEffectiveFrom() != null
        && request.getEffectiveTo() != null
        && request.getEffectiveFrom().isAfter(request.getEffectiveTo())) {
      throw new AppException(ErrorCode.LEAVE_APPROVAL_CONFIG_INVALID_EFFECTIVE_RANGE);
    }
  }

  private void validateUnique(LeaveApprovalConfigRequest request, Long excludeId) {
    boolean exists = leaveApprovalConfigRepository.existsByUniqueFields(
        request.getMilitaryPosition(), request.getEffectiveFrom(), request.getEffectiveTo(), excludeId);
    if (exists) {
      throw new AppException(ErrorCode.LEAVE_APPROVAL_CONFIG_DUPLICATE_UNIQUE_FIELDS);
    }
  }

  private void validateNoOverlap(LeaveApprovalConfigRequest request, Long excludeId) {
    boolean overlaps = leaveApprovalConfigRepository.existsOverlappingRange(
        request.getMilitaryPosition(), request.getEffectiveFrom(), request.getEffectiveTo(), excludeId);
    if (overlaps) {
      throw new AppException(ErrorCode.LEAVE_APPROVAL_CONFIG_OVERLAPPING_RANGE);
    }
  }

  private LeaveApprovalConfigResponse toResponse(LeaveApprovalConfig config) {
    return new LeaveApprovalConfigResponse(config);
  }
}
