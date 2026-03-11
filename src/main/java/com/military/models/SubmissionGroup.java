package com.military.models;

import com.military.payload.request.SubmissionGroupRequest;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class SubmissionGroup {
  private Long id;
  private String name;
  private String description;
  private List<Long> userIds = new ArrayList<>();

  public SubmissionGroup(SubmissionGroupRequest request) {
    BeanUtils.copyProperties(request, this);
  }
}
