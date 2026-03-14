package com.military.service.impl;

import com.military.exception.AppException;
import com.military.exception.ErrorCode;
import com.military.models.ELeaveRequestStatus;
import com.military.models.LeaveApprovalConfig;
import com.military.models.LeaveRequest;
import com.military.models.LeaveRequestHistory;
import com.military.models.MilitaryPersonnel;
import com.military.models.SubmissionFlow;
import com.military.models.SubmissionFlowGroup;
import com.military.models.SubmissionGroup;
import com.military.models.User;
import com.military.payload.request.LeaveRequestActionRequest;
import com.military.payload.request.LeaveRequestCreateRequest;
import com.military.payload.request.LeaveRequestUpdateRequest;
import com.military.payload.response.LeaveApprovalCapabilityResponse;
import com.military.payload.response.LeaveRequestHistoryResponse;
import com.military.payload.response.LeaveRequestResponse;
import com.military.repository.LeaveApprovalConfigRepository;
import com.military.repository.LeaveRequestHistoryRepository;
import com.military.repository.LeaveRequestRepository;
import com.military.repository.MilitaryPersonnelRepository;
import com.military.repository.SubmissionFlowRepository;
import com.military.repository.SubmissionGroupRepository;
import com.military.repository.UserRepository;
import com.military.security.services.UserDetailsImpl;
import com.military.service.LeaveRequestService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class LeaveRequestServiceImpl implements LeaveRequestService {
  private final LeaveRequestRepository leaveRequestRepository;
  private final LeaveRequestHistoryRepository leaveRequestHistoryRepository;
  private final UserRepository userRepository;
  private final MilitaryPersonnelRepository militaryPersonnelRepository;
  private final SubmissionFlowRepository submissionFlowRepository;
  private final SubmissionGroupRepository submissionGroupRepository;
  private final LeaveApprovalConfigRepository leaveApprovalConfigRepository;

  public LeaveRequestServiceImpl(LeaveRequestRepository leaveRequestRepository,
                                 LeaveRequestHistoryRepository leaveRequestHistoryRepository,
                                 UserRepository userRepository,
                                 MilitaryPersonnelRepository militaryPersonnelRepository,
                                 SubmissionFlowRepository submissionFlowRepository,
                                 SubmissionGroupRepository submissionGroupRepository,
                                 LeaveApprovalConfigRepository leaveApprovalConfigRepository) {
    this.leaveRequestRepository = leaveRequestRepository;
    this.leaveRequestHistoryRepository = leaveRequestHistoryRepository;
    this.userRepository = userRepository;
    this.militaryPersonnelRepository = militaryPersonnelRepository;
    this.submissionFlowRepository = submissionFlowRepository;
    this.submissionGroupRepository = submissionGroupRepository;
    this.leaveApprovalConfigRepository = leaveApprovalConfigRepository;
  }

  @Override
  public LeaveRequestResponse create(LeaveRequestCreateRequest request) {
    validateLeaveRange(request.getLeaveFrom(), request.getLeaveTo());
    CurrentUserContext currentUser = resolveCurrentUser();
    SubmissionFlow flow = resolveDefaultLeaveFlow();
    Integer requesterOrder = findRequesterOrder(flow, currentUser.user().getId());
    String requesterRound = formatRound(1, 1);
    int nextOrder = requesterOrder + 1;
    Assignee nextAssignee = resolveAssigneeByOrder(flow, nextOrder);
    String pendingRound = formatRound(1, 2);

    LeaveRequest leaveRequest = new LeaveRequest();
    leaveRequest.setMilitaryPersonnelId(currentUser.personnel().getId());
    leaveRequest.setUserId(currentUser.user().getId());
    leaveRequest.setCreatedAt(nowIso());
    leaveRequest.setLeaveFrom(request.getLeaveFrom());
    leaveRequest.setLeaveTo(request.getLeaveTo());
    leaveRequest.setFlowId(flow.getId());
    leaveRequest.setCurrentOrderNo(nextOrder);
    leaveRequest.setCurrentRound(pendingRound);
    leaveRequest.setCurrentAssignee(nextAssignee.username());
    leaveRequest.setStatus(ELeaveRequestStatus.CHUA_XU_LY);
    leaveRequest.setReason(request.getReason());
    LeaveRequest saved = leaveRequestRepository.save(leaveRequest);

    leaveRequestHistoryRepository.save(buildHistory(saved, requesterRound, currentUser.personnel().getId(),
        currentUser.user().getId(), currentUser.user().getUsername(), flow.getId(), requesterOrder,
        ELeaveRequestStatus.TRINH, request.getReason()));
    leaveRequestHistoryRepository.save(buildHistory(saved, pendingRound, currentUser.personnel().getId(),
        currentUser.user().getId(), nextAssignee.username(), flow.getId(), nextOrder,
        ELeaveRequestStatus.CHUA_XU_LY, null));

    return toResponse(saved);
  }

  @Override
  public LeaveRequestResponse getById(Long id) {
    LeaveRequest leaveRequest = findLeaveRequestById(id);
    CurrentUserContext currentUser = resolveCurrentUser();
    if (!canView(currentUser, leaveRequest)) {
      throw new AppException(ErrorCode.LEAVE_REQUEST_FORBIDDEN);
    }
    return toResponse(leaveRequest);
  }

  @Override
  public Page<LeaveRequestResponse> listMine(Pageable pageable) {
    CurrentUserContext currentUser = resolveCurrentUser();
    List<LeaveRequestResponse> data = leaveRequestRepository.findAllList().stream()
        .filter(item -> currentUser.user().getId().equals(item.getUserId()))
        .map(this::toResponse)
        .toList();
    return paginate(data, pageable);
  }

  @Override
  public Page<LeaveRequestResponse> listPendingApproval(Pageable pageable) {
    CurrentUserContext currentUser = resolveCurrentUser();
    String username = currentUser.user().getUsername();
    List<LeaveRequestResponse> data = leaveRequestRepository.findAllList().stream()
        .filter(item -> username.equalsIgnoreCase(item.getCurrentAssignee()))
        .filter(item -> item.getStatus() == ELeaveRequestStatus.CHUA_XU_LY
            || item.getStatus() == ELeaveRequestStatus.DA_TIEP_NHAN)
        .map(this::toResponse)
        .toList();
    return paginate(data, pageable);
  }

  @Override
  public List<LeaveRequestHistoryResponse> getHistories(Long id) {
    LeaveRequest leaveRequest = findLeaveRequestById(id);
    CurrentUserContext currentUser = resolveCurrentUser();
    if (!canView(currentUser, leaveRequest)) {
      throw new AppException(ErrorCode.LEAVE_REQUEST_FORBIDDEN);
    }
    return leaveRequestHistoryRepository.findAllByLeaveRequestId(id).stream()
        .sorted(this::compareRound)
        .map(LeaveRequestHistoryResponse::new)
        .toList();
  }

  @Override
  public LeaveRequestResponse accept(Long id, LeaveRequestActionRequest request) {
    LeaveRequest leaveRequest = findLeaveRequestById(id);
    CurrentUserContext currentUser = resolveCurrentUser();
    LeaveRequestHistory currentRound = findCurrentRound(leaveRequest);
    validateAssignee(currentRound, currentUser.user().getUsername());
    if (currentRound.getStatus() != ELeaveRequestStatus.CHUA_XU_LY) {
      throw new AppException(ErrorCode.LEAVE_REQUEST_INVALID_STATE);
    }

    currentRound.setStatus(ELeaveRequestStatus.DA_TIEP_NHAN);
    currentRound.setReason(request == null ? null : request.getReason());
    leaveRequestHistoryRepository.save(currentRound);

    leaveRequest.setStatus(ELeaveRequestStatus.DA_TIEP_NHAN);
    leaveRequest.setReason(request == null ? leaveRequest.getReason() : request.getReason());
    return toResponse(leaveRequestRepository.save(leaveRequest));
  }

  @Override
  public LeaveRequestResponse approve(Long id, LeaveRequestActionRequest request) {
    LeaveRequest leaveRequest = findLeaveRequestById(id);
    LeaveApprovalCapabilityResponse checkHierarchy = checkApprovalCapability(id);
    if (!checkHierarchy.isCanApprove()) {
      throw new AppException(ErrorCode.NO_APPROVE_AUTHORITY);
    }
    CurrentUserContext currentUser = resolveCurrentUser();
    LeaveRequestHistory currentRound = findCurrentRound(leaveRequest);
    validateAssignee(currentRound, currentUser.user().getUsername());
    if (currentRound.getStatus() != ELeaveRequestStatus.CHUA_XU_LY
        && currentRound.getStatus() != ELeaveRequestStatus.DA_TIEP_NHAN) {
      throw new AppException(ErrorCode.LEAVE_REQUEST_INVALID_STATE);
    }

    if (!canApprove(currentUser.personnel(), leaveRequest.getLeaveFrom(), leaveRequest.getLeaveTo())) {
      throw new AppException(ErrorCode.LEAVE_REQUEST_APPROVAL_LIMIT_EXCEEDED);
    }

    currentRound.setStatus(ELeaveRequestStatus.DA_DUYET);
    currentRound.setReason(request == null ? null : request.getReason());
    leaveRequestHistoryRepository.save(currentRound);

    leaveRequest.setStatus(ELeaveRequestStatus.DA_DUYET);
    leaveRequest.setReason(request == null ? leaveRequest.getReason() : request.getReason());
    leaveRequest.setCurrentAssignee(resolveRequesterUsername(leaveRequest.getUserId()));
    return toResponse(leaveRequestRepository.save(leaveRequest));
  }

  @Override
  public LeaveRequestResponse sendBack(Long id, LeaveRequestActionRequest request) {
    LeaveRequest leaveRequest = findLeaveRequestById(id);
    CurrentUserContext currentUser = resolveCurrentUser();
    LeaveRequestHistory currentRound = findCurrentRound(leaveRequest);
    validateAssignee(currentRound, currentUser.user().getUsername());
    if (currentRound.getStatus() != ELeaveRequestStatus.CHUA_XU_LY
        && currentRound.getStatus() != ELeaveRequestStatus.DA_TIEP_NHAN) {
      throw new AppException(ErrorCode.LEAVE_REQUEST_INVALID_STATE);
    }

    currentRound.setStatus(ELeaveRequestStatus.TRA_VE);
    currentRound.setReason(request == null ? null : request.getReason());
    leaveRequestHistoryRepository.save(currentRound);

    leaveRequest.setStatus(ELeaveRequestStatus.TRA_VE);
    leaveRequest.setReason(request == null ? leaveRequest.getReason() : request.getReason());
    leaveRequest.setCurrentAssignee(resolveRequesterUsername(leaveRequest.getUserId()));
    return toResponse(leaveRequestRepository.save(leaveRequest));
  }

  @Override
  public LeaveRequestResponse updateForResubmit(Long id, LeaveRequestUpdateRequest request) {
    LeaveRequest leaveRequest = findLeaveRequestById(id);
    CurrentUserContext currentUser = resolveCurrentUser();
    validateRequester(leaveRequest, currentUser.user().getId());
    if (leaveRequest.getStatus() != ELeaveRequestStatus.TRA_VE
        && leaveRequest.getStatus() != ELeaveRequestStatus.MO_YEU_CAU) {
      throw new AppException(ErrorCode.LEAVE_REQUEST_INVALID_STATE);
    }
    validateLeaveRange(request.getLeaveFrom(), request.getLeaveTo());

    leaveRequest.setLeaveFrom(request.getLeaveFrom());
    leaveRequest.setLeaveTo(request.getLeaveTo());
    leaveRequest.setReason(request.getReason());
    leaveRequest.setStatus(ELeaveRequestStatus.MO_YEU_CAU);
    leaveRequest.setCurrentAssignee(currentUser.user().getUsername());
    return toResponse(leaveRequestRepository.save(leaveRequest));
  }

  @Override
  public LeaveRequestResponse resubmit(Long id, LeaveRequestActionRequest request) {
    LeaveRequest leaveRequest = findLeaveRequestById(id);
    CurrentUserContext currentUser = resolveCurrentUser();
    validateRequester(leaveRequest, currentUser.user().getId());
    if (leaveRequest.getStatus() != ELeaveRequestStatus.TRA_VE
        && leaveRequest.getStatus() != ELeaveRequestStatus.MO_YEU_CAU) {
      throw new AppException(ErrorCode.LEAVE_REQUEST_INVALID_STATE);
    }

    SubmissionFlow flow = submissionFlowRepository.findById(leaveRequest.getFlowId())
        .orElseThrow(() -> new AppException(ErrorCode.SUBMISSION_FLOW_NOT_FOUND));
    Integer requesterOrder = findRequesterOrder(flow, currentUser.user().getId());
    int nextOrder = requesterOrder + 1;
    Assignee nextAssignee = resolveAssigneeByOrder(flow, nextOrder);

    Round currentRound = parseRound(leaveRequest.getCurrentRound());
    int major = currentRound.major();
    int maxSeqInMajor = leaveRequestHistoryRepository.findAllByLeaveRequestId(leaveRequest.getId()).stream()
        .map(LeaveRequestHistory::getRoundNo)
        .map(this::parseRound)
        .filter(round -> round.major() == major)
        .map(Round::sequence)
        .max(Integer::compareTo)
        .orElse(0);

    String submitRound = formatRound(major, maxSeqInMajor + 1);
    String pendingRound = formatRound(major, maxSeqInMajor + 2);

    leaveRequestHistoryRepository.save(buildHistory(leaveRequest, submitRound, leaveRequest.getMilitaryPersonnelId(),
        leaveRequest.getUserId(), currentUser.user().getUsername(), leaveRequest.getFlowId(), requesterOrder,
        ELeaveRequestStatus.TRINH, request == null ? leaveRequest.getReason() : request.getReason()));
    leaveRequestHistoryRepository.save(buildHistory(leaveRequest, pendingRound, leaveRequest.getMilitaryPersonnelId(),
        leaveRequest.getUserId(), nextAssignee.username(), leaveRequest.getFlowId(), nextOrder,
        ELeaveRequestStatus.CHUA_XU_LY, null));

    leaveRequest.setStatus(ELeaveRequestStatus.CHUA_XU_LY);
    leaveRequest.setCurrentOrderNo(nextOrder);
    leaveRequest.setCurrentRound(pendingRound);
    leaveRequest.setCurrentAssignee(nextAssignee.username());
    return toResponse(leaveRequestRepository.save(leaveRequest));
  }

  @Override
  public LeaveRequestResponse openSupplement(Long id, LeaveRequestActionRequest request) {
    LeaveRequest leaveRequest = findLeaveRequestById(id);
    CurrentUserContext currentUser = resolveCurrentUser();
    validateRequester(leaveRequest, currentUser.user().getId());
    if (leaveRequest.getStatus() != ELeaveRequestStatus.DA_DUYET) {
      throw new AppException(ErrorCode.LEAVE_REQUEST_INVALID_STATE);
    }

    Round latestRound = parseRound(leaveRequest.getCurrentRound());
    int nextMajor = latestRound.major() + 1;
    String roundNo = formatRound(nextMajor, 1);
    SubmissionFlow flow = submissionFlowRepository.findById(leaveRequest.getFlowId())
        .orElseThrow(() -> new AppException(ErrorCode.SUBMISSION_FLOW_NOT_FOUND));
    Integer requesterOrder = findRequesterOrder(flow, currentUser.user().getId());

    leaveRequestHistoryRepository.save(buildHistory(leaveRequest, roundNo, leaveRequest.getMilitaryPersonnelId(),
        leaveRequest.getUserId(), currentUser.user().getUsername(), leaveRequest.getFlowId(), requesterOrder,
        ELeaveRequestStatus.MO_YEU_CAU, request == null ? null : request.getReason()));

    leaveRequest.setStatus(ELeaveRequestStatus.MO_YEU_CAU);
    leaveRequest.setCurrentRound(roundNo);
    leaveRequest.setCurrentOrderNo(requesterOrder);
    leaveRequest.setCurrentAssignee(currentUser.user().getUsername());
    leaveRequest.setReason(request == null ? leaveRequest.getReason() : request.getReason());
    return toResponse(leaveRequestRepository.save(leaveRequest));
  }

  @Override
  public LeaveRequestResponse submitNext(Long id, LeaveRequestActionRequest request) {
    LeaveRequest leaveRequest = findLeaveRequestById(id);
    CurrentUserContext currentUser = resolveCurrentUser();
    LeaveRequestHistory currentRound = findCurrentRound(leaveRequest);
    validateAssignee(currentRound, currentUser.user().getUsername());
    if (currentRound.getStatus() != ELeaveRequestStatus.CHUA_XU_LY
        && currentRound.getStatus() != ELeaveRequestStatus.DA_TIEP_NHAN) {
      throw new AppException(ErrorCode.LEAVE_REQUEST_INVALID_STATE);
    }

    SubmissionFlow flow = submissionFlowRepository.findById(leaveRequest.getFlowId())
        .orElseThrow(() -> new AppException(ErrorCode.SUBMISSION_FLOW_NOT_FOUND));
    int nextOrder = currentRound.getOrderNo() + 1;
    Assignee nextAssignee = resolveAssigneeByOrder(flow, nextOrder);

    Round round = parseRound(currentRound.getRoundNo());
    String nextRoundNo = formatRound(round.major(), round.sequence() + 1);

    currentRound.setStatus(ELeaveRequestStatus.TRINH);
    currentRound.setReason(request == null ? null : request.getReason());
    leaveRequestHistoryRepository.save(currentRound);
    leaveRequestHistoryRepository.save(buildHistory(leaveRequest, nextRoundNo, leaveRequest.getMilitaryPersonnelId(),
        leaveRequest.getUserId(), nextAssignee.username(), leaveRequest.getFlowId(), nextOrder,
        ELeaveRequestStatus.CHUA_XU_LY, null));

    leaveRequest.setStatus(ELeaveRequestStatus.CHUA_XU_LY);
    leaveRequest.setCurrentOrderNo(nextOrder);
    leaveRequest.setCurrentRound(nextRoundNo);
    leaveRequest.setCurrentAssignee(nextAssignee.username());
    leaveRequest.setReason(request == null ? leaveRequest.getReason() : request.getReason());
    return toResponse(leaveRequestRepository.save(leaveRequest));
  }

  @Override
  public LeaveApprovalCapabilityResponse checkApprovalCapability(Long leaveRequestId) {
    CurrentUserContext currentUser = resolveCurrentUser();
    LeaveApprovalCapabilityResponse response = new LeaveApprovalCapabilityResponse();
    response.setMilitaryPosition(currentUser.personnel().getPositionCode() == null
        ? null : currentUser.personnel().getPositionCode().name());
    response.setLeaveRequestId(leaveRequestId);

    if (leaveRequestId != null) {
      LeaveRequest leaveRequest = findLeaveRequestById(leaveRequestId);
      if (!canView(currentUser, leaveRequest)) {
        throw new AppException(ErrorCode.LEAVE_REQUEST_FORBIDDEN);
      }
      LocalDate leaveFrom = leaveRequest.getLeaveFrom();
      LocalDate leaveTo = leaveRequest.getLeaveTo();

      validateLeaveRange(leaveFrom, leaveTo);
      Integer requestedDays = (int) ChronoUnit.DAYS.between(leaveFrom, leaveTo) + 1;

      LeaveApprovalConfig config = leaveApprovalConfigRepository
              .findApplicable(currentUser.personnel().getPositionCode(), LocalDate.now())
              .orElse(null);

      if (config == null || config.getMaxApprovalDays() == null || config.getMaxApprovalDays() <= 0) {
        response.setCanApprove(false);
        response.setRequestedDays(requestedDays);
        response.setReason("Yêu cầu thuộc thẩm quyền phê duyệt của cấp trên!");
        return response;
      }

      response.setMaxApprovalDays(config.getMaxApprovalDays());
      response.setRequestedDays(requestedDays);
      boolean canApprove = requestedDays <= config.getMaxApprovalDays();
      response.setCanApprove(canApprove);
      response.setReason(canApprove
              ? ""
              : "Yêu cầu thuộc thẩm quyền phê duyệt của cấp trên!");
    }
    return response;
  }

  private boolean canView(CurrentUserContext currentUser, LeaveRequest leaveRequest) {
    return currentUser.user().getId().equals(leaveRequest.getUserId())
        || currentUser.user().getUsername().equalsIgnoreCase(leaveRequest.getCurrentAssignee());
  }

  private LeaveRequest findLeaveRequestById(Long id) {
    return leaveRequestRepository.findById(id)
        .orElseThrow(() -> new AppException(ErrorCode.LEAVE_REQUEST_NOT_FOUND));
  }

  private LeaveRequestHistory findCurrentRound(LeaveRequest leaveRequest) {
    return leaveRequestHistoryRepository.findAllByLeaveRequestId(leaveRequest.getId()).stream()
        .filter(item -> item.getRoundNo() != null && item.getRoundNo().equals(leaveRequest.getCurrentRound()))
        .findFirst()
        .orElseThrow(() -> new AppException(ErrorCode.LEAVE_REQUEST_HISTORY_NOT_FOUND));
  }

  private void validateAssignee(LeaveRequestHistory history, String username) {
    if (history.getAssignee() == null || !history.getAssignee().equalsIgnoreCase(username)) {
      throw new AppException(ErrorCode.LEAVE_REQUEST_FORBIDDEN);
    }
  }

  private void validateRequester(LeaveRequest leaveRequest, Long userId) {
    if (leaveRequest.getUserId() == null || !leaveRequest.getUserId().equals(userId)) {
      throw new AppException(ErrorCode.LEAVE_REQUEST_FORBIDDEN);
    }
  }

  private void validateLeaveRange(LocalDate leaveFrom, LocalDate leaveTo) {
    if (leaveFrom == null || leaveTo == null || leaveFrom.isAfter(leaveTo)) {
      throw new AppException(ErrorCode.LEAVE_REQUEST_INVALID_RANGE);
    }
  }

  private SubmissionFlow resolveDefaultLeaveFlow() {
    List<SubmissionFlow> flows = submissionFlowRepository.findAll(PageRequest.of(0, 1000)).getContent();
    Optional<SubmissionFlow> leaveFlowByCode = flows.stream()
        .filter(flow -> flow.getCode() != null)
        .filter(flow -> {
          String value = flow.getCode().toLowerCase(Locale.ROOT);
          return value.equals("leave") || value.equals("nghi_phep");
        })
        .findFirst();
    if (leaveFlowByCode.isPresent()) {
      return leaveFlowByCode.get();
    }
    throw new AppException(ErrorCode.LEAVE_REQUEST_DEFAULT_FLOW_NOT_FOUND);
  }

  private Integer findRequesterOrder(SubmissionFlow flow, Long userId) {
    if (flow.getGroups() == null || flow.getGroups().isEmpty()) {
      throw new AppException(ErrorCode.SUBMISSION_FLOW_INVALID_GROUPS);
    }
    return flow.getGroups().stream()
        .sorted(Comparator.comparing(SubmissionFlowGroup::getOrderNo))
        .filter(group -> containsUser(group.getGroupId(), userId))
        .map(SubmissionFlowGroup::getOrderNo)
        .findFirst()
        .orElseThrow(() -> new AppException(ErrorCode.LEAVE_REQUEST_REQUESTER_NOT_IN_FLOW));
  }

  private Assignee resolveAssigneeByOrder(SubmissionFlow flow, Integer orderNo) {
    SubmissionFlowGroup target = flow.getGroups().stream()
        .filter(group -> orderNo.equals(group.getOrderNo()))
        .findFirst()
        .orElseThrow(() -> new AppException(ErrorCode.LEAVE_REQUEST_NEXT_ASSIGNEE_NOT_FOUND));
    SubmissionGroup submissionGroup = submissionGroupRepository.findById(target.getGroupId())
        .orElseThrow(() -> new AppException(ErrorCode.SUBMISSION_GROUP_NOT_FOUND));
    if (submissionGroup.getUserIds() == null || submissionGroup.getUserIds().isEmpty()) {
      throw new AppException(ErrorCode.LEAVE_REQUEST_NEXT_ASSIGNEE_NOT_FOUND);
    }
    Long userId = submissionGroup.getUserIds().get(0);
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new AppException(ErrorCode.SUBMISSION_FLOW_USER_NOT_FOUND));
    return new Assignee(user.getId(), user.getUsername());
  }

  private boolean containsUser(Long groupId, Long userId) {
    SubmissionGroup group = submissionGroupRepository.findById(groupId)
        .orElseThrow(() -> new AppException(ErrorCode.SUBMISSION_GROUP_NOT_FOUND));
    return group.getUserIds() != null && group.getUserIds().stream().anyMatch(userId::equals);
  }

  private boolean canApprove(MilitaryPersonnel approver, LocalDate leaveFrom, LocalDate leaveTo) {
    if (approver.getPositionCode() == null) {
      return false;
    }
    int leaveDays = (int) ChronoUnit.DAYS.between(leaveFrom, leaveTo) + 1;
    LeaveApprovalConfig config = leaveApprovalConfigRepository.findApplicable(approver.getPositionCode(), LocalDate.now())
        .orElse(null);
    if (config == null || config.getMaxApprovalDays() == null) {
      return false;
    }
    return leaveDays <= config.getMaxApprovalDays();
  }

  private String resolveRequesterUsername(Long requesterUserId) {
    User user = userRepository.findById(requesterUserId)
        .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED));
    return user.getUsername();
  }

  private CurrentUserContext resolveCurrentUser() {
    Object principal = org.springframework.security.core.context.SecurityContextHolder.getContext()
        .getAuthentication()
        .getPrincipal();
    if (!(principal instanceof UserDetailsImpl userDetails)) {
      throw new AppException(ErrorCode.UNAUTHORIZED);
    }
    User user = userRepository.findById(userDetails.getId())
        .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED));
    if (user.getMilitaryPersonnelId() == null) {
      throw new AppException(ErrorCode.PERSONNEL_NOT_FOUND);
    }
    MilitaryPersonnel personnel = militaryPersonnelRepository.findById(user.getMilitaryPersonnelId())
        .orElseThrow(() -> new AppException(ErrorCode.PERSONNEL_NOT_FOUND));
    return new CurrentUserContext(user, personnel);
  }

  private LeaveRequestHistory buildHistory(LeaveRequest leaveRequest,
                                           String roundNo,
                                           Long militaryPersonnelId,
                                           Long userId,
                                           String assignee,
                                           Long flowId,
                                           Integer orderNo,
                                           ELeaveRequestStatus status,
                                           String reason) {
    LeaveRequestHistory history = new LeaveRequestHistory();
    history.setLeaveRequestId(leaveRequest.getId());
    history.setRoundNo(roundNo);
    history.setMilitaryPersonnelId(militaryPersonnelId);
    history.setUserId(userId);
    history.setCreatedAt(nowIso());
    history.setLeaveFrom(leaveRequest.getLeaveFrom());
    history.setLeaveTo(leaveRequest.getLeaveTo());
    history.setStatus(status);
    history.setAssignee(assignee);
    history.setFlowId(flowId);
    history.setOrderNo(orderNo);
    history.setReason(reason);
    return history;
  }

  private Page<LeaveRequestResponse> paginate(List<LeaveRequestResponse> data, Pageable pageable) {
    int start = (int) pageable.getOffset();
    if (start >= data.size()) {
      return new PageImpl<>(List.of(), pageable, data.size());
    }
    int end = Math.min(start + pageable.getPageSize(), data.size());
    return new PageImpl<>(data.subList(start, end), pageable, data.size());
  }

  private int compareRound(LeaveRequestHistory left, LeaveRequestHistory right) {
    Round round1 = parseRound(left.getRoundNo());
    Round round2 = parseRound(right.getRoundNo());
    if (round1.major() != round2.major()) {
      return Integer.compare(round1.major(), round2.major());
    }
    return Integer.compare(round1.sequence(), round2.sequence());
  }

  private Round parseRound(String roundNo) {
    if (roundNo == null || !roundNo.contains(".")) {
      throw new AppException(ErrorCode.LEAVE_REQUEST_HISTORY_NOT_FOUND);
    }
    String[] parts = roundNo.split("\\.");
    if (parts.length != 2) {
      throw new AppException(ErrorCode.LEAVE_REQUEST_HISTORY_NOT_FOUND);
    }
    try {
      int major = Integer.parseInt(parts[0]);
      int sequence = Integer.parseInt(parts[1]);
      return new Round(major, sequence);
    } catch (NumberFormatException ex) {
      throw new AppException(ErrorCode.LEAVE_REQUEST_HISTORY_NOT_FOUND);
    }
  }

  private String formatRound(int major, int sequence) {
    return major + "." + String.format("%04d", sequence);
  }

  private String nowIso() {
    return Instant.now().toString();
  }

  private LeaveRequestResponse toResponse(LeaveRequest leaveRequest) {
    return new LeaveRequestResponse(leaveRequest);
  }

  private record CurrentUserContext(User user, MilitaryPersonnel personnel) {
  }

  private record Assignee(Long userId, String username) {
  }

  private record Round(int major, int sequence) {
  }
}
