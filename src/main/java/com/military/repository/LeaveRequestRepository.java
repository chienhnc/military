package com.military.repository;

import com.military.models.LeaveRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveRequestRepository {
  LeaveRequest save(LeaveRequest leaveRequest);

  Optional<LeaveRequest> findById(Long id);

  Page<LeaveRequest> findAll(Pageable pageable);

  List<LeaveRequest> findAllList();
}
