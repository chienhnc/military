package com.military.service.impl;

import com.military.exception.AppException;
import com.military.exception.ErrorCode;
import com.military.models.SubmissionFlow;
import com.military.models.SubmissionFlowGroup;
import com.military.payload.request.SubmissionFlowGroupRequest;
import com.military.payload.request.SubmissionFlowRequest;
import com.military.payload.response.SubmissionFlowResponse;
import com.military.repository.SubmissionGroupRepository;
import com.military.repository.SubmissionFlowRepository;
import com.military.service.SubmissionFlowService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashSet;

@Service
public class SubmissionFlowServiceImpl implements SubmissionFlowService {
  private final SubmissionFlowRepository submissionFlowRepository;
  private final SubmissionGroupRepository submissionGroupRepository;

  public SubmissionFlowServiceImpl(SubmissionFlowRepository submissionFlowRepository,
                                   SubmissionGroupRepository submissionGroupRepository) {
    this.submissionFlowRepository = submissionFlowRepository;
    this.submissionGroupRepository = submissionGroupRepository;
  }

  @Override
  public SubmissionFlowResponse create(SubmissionFlowRequest request) {
    SubmissionFlow flow = new SubmissionFlow();
    flow.setName(request.getName());
    flow.setDescription(request.getDescription());
    flow.setGroups(validateAndBuildGroups(request.getGroups()));
    return toResponse(submissionFlowRepository.save(flow));
  }

  @Override
  public SubmissionFlowResponse update(Long id, SubmissionFlowRequest request) {
    SubmissionFlow flow = findEntityById(id);
    flow.setName(request.getName());
    flow.setDescription(request.getDescription());
    flow.setGroups(validateAndBuildGroups(request.getGroups()));
    return toResponse(submissionFlowRepository.save(flow));
  }

  @Override
  public SubmissionFlowResponse getById(Long id) {
    return toResponse(findEntityById(id));
  }

  @Override
  public Page<SubmissionFlowResponse> list(String keyword, Pageable pageable) {
    if (keyword == null || keyword.trim().isEmpty()) {
      return submissionFlowRepository.findAll(pageable).map(this::toResponse);
    }
    return submissionFlowRepository.findByNameContainingIgnoreCase(keyword.trim(), pageable).map(this::toResponse);
  }

  @Override
  public void delete(Long id) {
    submissionFlowRepository.delete(findEntityById(id));
  }

  private SubmissionFlow findEntityById(Long id) {
    return submissionFlowRepository.findById(id)
        .orElseThrow(() -> new AppException(ErrorCode.SUBMISSION_FLOW_NOT_FOUND));
  }

  private List<SubmissionFlowGroup> validateAndBuildGroups(List<SubmissionFlowGroupRequest> requests) {
    if (requests == null || requests.isEmpty()) {
      throw new AppException(ErrorCode.SUBMISSION_FLOW_INVALID_GROUPS);
    }

    List<SubmissionFlowGroupRequest> sorted = new ArrayList<>(requests);
    sorted.sort(Comparator.comparing(SubmissionFlowGroupRequest::getOrderNo));
    for (int i = 0; i < sorted.size(); i++) {
      int expectedOrder = i + 1;
      SubmissionFlowGroupRequest group = sorted.get(i);
      if (group.getOrderNo() == null || group.getOrderNo() != expectedOrder) {
        throw new AppException(ErrorCode.SUBMISSION_FLOW_INVALID_GROUPS);
      }
      if (group.getGroupId() == null) {
        throw new AppException(ErrorCode.SUBMISSION_FLOW_INVALID_GROUPS);
      }
    }

    List<SubmissionFlowGroup> groups = new ArrayList<>();
    for (SubmissionFlowGroupRequest request : sorted) {
      if (submissionGroupRepository.findById(request.getGroupId()).isEmpty()) {
        throw new AppException(ErrorCode.SUBMISSION_GROUP_NOT_FOUND);
      }
      groups.add(new SubmissionFlowGroup(request.getOrderNo(), request.getGroupId()));
    }

    LinkedHashSet<Long> uniqueGroupIds = groups.stream()
        .map(SubmissionFlowGroup::getGroupId)
        .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    if (uniqueGroupIds.size() != groups.size()) {
      throw new AppException(ErrorCode.SUBMISSION_FLOW_INVALID_GROUPS);
    }

    return groups;
  }

  private SubmissionFlowResponse toResponse(SubmissionFlow flow) {
    return new SubmissionFlowResponse(flow);
  }
}
