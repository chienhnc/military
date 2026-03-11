package com.military.service.impl;

import com.military.exception.AppException;
import com.military.exception.ErrorCode;
import com.military.models.SubmissionGroup;
import com.military.payload.request.SubmissionGroupRequest;
import com.military.payload.response.SubmissionGroupResponse;
import com.military.repository.SubmissionFlowRepository;
import com.military.repository.SubmissionGroupRepository;
import com.military.repository.UserRepository;
import com.military.service.SubmissionGroupService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class SubmissionGroupServiceImpl implements SubmissionGroupService {
  private final SubmissionGroupRepository submissionGroupRepository;
  private final SubmissionFlowRepository submissionFlowRepository;
  private final UserRepository userRepository;

  public SubmissionGroupServiceImpl(SubmissionGroupRepository submissionGroupRepository,
                                    SubmissionFlowRepository submissionFlowRepository,
                                    UserRepository userRepository) {
    this.submissionGroupRepository = submissionGroupRepository;
    this.submissionFlowRepository = submissionFlowRepository;
    this.userRepository = userRepository;
  }

  @Override
  public SubmissionGroupResponse create(SubmissionGroupRequest request) {
    SubmissionGroup group = new SubmissionGroup(request);
    group.setUserIds(new ArrayList<>());
    return toResponse(submissionGroupRepository.save(group));
  }

  @Override
  public SubmissionGroupResponse update(Long id, SubmissionGroupRequest request) {
    SubmissionGroup group = findEntityById(id);
    group.setName(request.getName());
    group.setDescription(request.getDescription());
    return toResponse(submissionGroupRepository.save(group));
  }

  @Override
  public SubmissionGroupResponse getById(Long id) {
    return toResponse(findEntityById(id));
  }

  @Override
  public Page<SubmissionGroupResponse> list(String keyword, Pageable pageable) {
    if (keyword == null || keyword.trim().isEmpty()) {
      return submissionGroupRepository.findAll(pageable).map(this::toResponse);
    }
    return submissionGroupRepository.findByNameContainingIgnoreCase(keyword.trim(), pageable).map(this::toResponse);
  }

  @Override
  public void delete(Long id) {
    SubmissionGroup group = findEntityById(id);
    boolean usedInFlow = submissionFlowRepository.existsByGroupId(id);
    if (usedInFlow) {
      throw new AppException(ErrorCode.SUBMISSION_GROUP_IN_USE);
    }
    submissionGroupRepository.delete(group);
  }

  @Override
  public SubmissionGroupResponse addUsers(Long id, List<Long> userIds) {
    SubmissionGroup group = findEntityById(id);
    LinkedHashSet<Long> merged = new LinkedHashSet<>(group.getUserIds() == null ? List.of() : group.getUserIds());
    validateUsers(userIds);
    merged.addAll(userIds);
    group.setUserIds(new ArrayList<>(merged));
    return toResponse(submissionGroupRepository.save(group));
  }

  @Override
  public SubmissionGroupResponse removeUsers(Long id, List<Long> userIds) {
    SubmissionGroup group = findEntityById(id);
    if (group.getUserIds() == null || group.getUserIds().isEmpty()) {
      return toResponse(group);
    }
    LinkedHashSet<Long> current = new LinkedHashSet<>(group.getUserIds());
    current.removeAll(userIds == null ? List.of() : userIds);
    group.setUserIds(new ArrayList<>(current));
    return toResponse(submissionGroupRepository.save(group));
  }

  private SubmissionGroup findEntityById(Long id) {
    return submissionGroupRepository.findById(id)
        .orElseThrow(() -> new AppException(ErrorCode.SUBMISSION_GROUP_NOT_FOUND));
  }

  private void validateUsers(List<Long> userIds) {
    if (userIds == null || userIds.isEmpty()) {
      throw new AppException(ErrorCode.SUBMISSION_GROUP_INVALID_USERS);
    }
    for (Long userId : userIds) {
      if (userId == null || userRepository.findById(userId).isEmpty()) {
        throw new AppException(ErrorCode.SUBMISSION_FLOW_USER_NOT_FOUND);
      }
    }
  }

  private SubmissionGroupResponse toResponse(SubmissionGroup group) {
    return new SubmissionGroupResponse(group);
  }
}
