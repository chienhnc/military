package com.military.payload.response;

import com.military.models.SubmissionFlowGroup;
import lombok.Data;
import org.springframework.beans.BeanUtils;

@Data
public class SubmissionFlowGroupResponse {
  private Integer orderNo;
  private Long groupId;

  public SubmissionFlowGroupResponse(SubmissionFlowGroup group) {
    BeanUtils.copyProperties(group, this);
  }
}
