package com.military.repository;

import com.military.models.SubmissionGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubmissionGroupRepository {
  SubmissionGroup save(SubmissionGroup group);

  Optional<SubmissionGroup> findById(Long id);

  Page<SubmissionGroup> findAll(Pageable pageable);

  Page<SubmissionGroup> findByNameContainingIgnoreCase(String keyword, Pageable pageable);

  void delete(SubmissionGroup group);
}
