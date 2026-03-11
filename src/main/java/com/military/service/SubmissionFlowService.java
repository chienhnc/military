package com.military.service;

import com.military.payload.request.SubmissionFlowRequest;
import com.military.payload.response.SubmissionFlowResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SubmissionFlowService {
  SubmissionFlowResponse create(SubmissionFlowRequest request);

  SubmissionFlowResponse update(Long id, SubmissionFlowRequest request);

  SubmissionFlowResponse getById(Long id);

  Page<SubmissionFlowResponse> list(String keyword, Pageable pageable);

  void delete(Long id);
}
