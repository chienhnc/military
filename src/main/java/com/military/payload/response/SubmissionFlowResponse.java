package com.military.payload.response;

import com.military.models.SubmissionFlow;
import com.military.models.SubmissionFlowGroup;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Data
public class SubmissionFlowResponse {
  private Long id;
  private String name;
  private String description;
  private List<SubmissionFlowGroupResponse> groups = new ArrayList<>();

  public SubmissionFlowResponse(SubmissionFlow flow) {
    BeanUtils.copyProperties(flow, this);
    if (flow.getGroups() != null) {
      this.groups = flow.getGroups().stream()
          .sorted(Comparator.comparing(SubmissionFlowGroup::getOrderNo, Comparator.nullsLast(Integer::compareTo)))
          .map(SubmissionFlowGroupResponse::new)
          .toList();
    }
  }
}
