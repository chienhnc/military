package com.military.repository;

import com.military.models.SubmissionFlow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubmissionFlowRepository {
  SubmissionFlow save(SubmissionFlow flow);

  Optional<SubmissionFlow> findById(Long id);

  Page<SubmissionFlow> findAll(Pageable pageable);

  void delete(SubmissionFlow flow);

  Page<SubmissionFlow> findByNameContainingIgnoreCase(String nameKeyword, Pageable pageable);

  boolean existsByGroupId(Long groupId);

  boolean existsByCodeIgnoreCase(String code, Long excludeId);
}
