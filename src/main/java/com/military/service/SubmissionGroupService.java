package com.military.service;

import com.military.payload.request.SubmissionGroupRequest;
import com.military.payload.response.SubmissionGroupResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SubmissionGroupService {
  SubmissionGroupResponse create(SubmissionGroupRequest request);

  SubmissionGroupResponse update(Long id, SubmissionGroupRequest request);

  SubmissionGroupResponse getById(Long id);

  Page<SubmissionGroupResponse> list(String keyword, Pageable pageable);

  void delete(Long id);

  SubmissionGroupResponse addUsers(Long id, List<Long> userIds);

  SubmissionGroupResponse removeUsers(Long id, List<Long> userIds);
}
