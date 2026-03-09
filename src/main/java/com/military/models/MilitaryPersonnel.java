package com.military.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.military.payload.request.MilitaryPersonnelRequest;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.beans.BeanUtils;

@Entity
@Table(name = "military_personnel", uniqueConstraints = {
    @UniqueConstraint(columnNames = "code")
})
@Data
@NoArgsConstructor
public class MilitaryPersonnel {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "code", nullable = false, length = 300)
  private String code;

  @NotBlank
  @Size(max = 200)
  @Column(name = "full_name", nullable = false)
  private String fullName;

  @NotBlank
  @Size(max = 100)
  @Column(name = "rank_code", nullable = false)
  private String rankCode;

  @NotBlank
  @Size(max = 150)
  @Column(name = "unit_code", nullable = false)
  private String unitCode;

  @NotBlank
  @Size(max = 150)
  @Column(name = "position_Code", nullable = false)
  private String positionCode;

  @Column(name = "qr_code", nullable = false, columnDefinition = "LONGTEXT")
  private String qrCode;

  @Column(name = "image_path")
  private String imagePath;

  @OneToOne(mappedBy = "militaryPersonnel", fetch = FetchType.LAZY)
  @JsonIgnore
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private User user;

  public MilitaryPersonnel(MilitaryPersonnelRequest militaryPersonnelRequest) {
    BeanUtils.copyProperties(militaryPersonnelRequest, this);
  }
}
