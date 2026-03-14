package com.military.repository;

import com.military.models.EMilitaryPosition;
import com.military.models.LeaveApprovalConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface LeaveApprovalConfigRepository {
  LeaveApprovalConfig save(LeaveApprovalConfig config);

  Optional<LeaveApprovalConfig> findById(Long id);

  Page<LeaveApprovalConfig> findAll(Pageable pageable);

  void delete(LeaveApprovalConfig config);

  Page<LeaveApprovalConfig> findByFilters(EMilitaryPosition militaryPosition,
                                          Boolean active,
                                          LocalDate effectiveFrom,
                                          LocalDate effectiveTo,
                                          Pageable pageable);

  Optional<LeaveApprovalConfig> findApplicable(EMilitaryPosition militaryPosition, LocalDate applyDate);

  boolean existsByUniqueFields(EMilitaryPosition militaryPosition,
                               LocalDate effectiveFrom,
                               LocalDate effectiveTo,
                               Long excludeId);

  boolean existsOverlappingRange(EMilitaryPosition militaryPosition,
                                 LocalDate effectiveFrom,
                                 LocalDate effectiveTo,
                                 Long excludeId);
}
