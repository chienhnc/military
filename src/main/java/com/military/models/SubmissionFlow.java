package com.military.models;

import com.military.payload.request.SubmissionFlowRequest;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class SubmissionFlow {
  private Long id;
  private String name;
  private String description;
  private List<SubmissionFlowGroup> groups = new ArrayList<>();

  public SubmissionFlow(SubmissionFlowRequest request) {
    BeanUtils.copyProperties(request, this);
  }
}
