package com.military.repository;

import com.military.models.LeaveRequestHistory;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveRequestHistoryRepository {
  LeaveRequestHistory save(LeaveRequestHistory history);

  Optional<LeaveRequestHistory> findById(Long id);

  List<LeaveRequestHistory> findAllByLeaveRequestId(Long leaveRequestId);
}
