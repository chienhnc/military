package com.military.payload.response;

import com.military.models.SubmissionGroup;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;

@Data
public class SubmissionGroupResponse {
  private Long id;
  private String name;
  private String description;
  private List<Long> userIds = new ArrayList<>();

  public SubmissionGroupResponse(SubmissionGroup group) {
    BeanUtils.copyProperties(group, this);
  }
}
