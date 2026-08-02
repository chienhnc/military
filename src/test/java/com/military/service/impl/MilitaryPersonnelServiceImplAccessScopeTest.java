package com.military.service.impl;

import com.google.gson.Gson;
import com.military.exception.AppException;
import com.military.models.MilitaryPersonnel;
import com.military.models.User;
import com.military.payload.response.MilitaryPersonnelResponse;
import com.military.repository.MilitaryPersonnelRepository;
import com.military.repository.MilitaryUnitRepository;
import com.military.repository.UserRepository;
import com.military.repository.VehicleRepository;
import com.military.security.services.UserDetailsImpl;
import com.military.service.VehicleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MilitaryPersonnelServiceImpl ROLE_USER access scope Tests")
class MilitaryPersonnelServiceImplAccessScopeTest {

  @Mock
  private MilitaryPersonnelRepository militaryPersonnelRepository;

  @Mock
  private MilitaryUnitRepository militaryUnitRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private VehicleService vehicleService;

  @Mock
  private VehicleRepository vehicleRepository;

  private MilitaryPersonnelServiceImpl service;

  @BeforeEach
  void setUp() {
    S3Client s3Client = mock(S3Client.class);
    service = new MilitaryPersonnelServiceImpl(
        militaryPersonnelRepository,
        militaryUnitRepository,
        userRepository,
        vehicleService,
        vehicleRepository,
        s3Client,
        "test-bucket",
        "personnel",
        new Gson()
    );
    lenient().when(vehicleRepository.findByPersonnelId(org.mockito.ArgumentMatchers.anyLong()))
        .thenReturn(Optional.empty());
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void authenticateAs(Long userId, String... roles) {
    List<SimpleGrantedAuthority> authorities = Arrays.stream(roles)
        .map(SimpleGrantedAuthority::new)
        .toList();
    UserDetailsImpl principal = new UserDetailsImpl(userId, "tester", "tester@example.com", "password", authorities);
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, null, authorities));
  }

  @Test
  @DisplayName("ROLE_USER list() should see every personnel in the same unit, not only themselves")
  void testRoleUserSeesWholeUnitInList() {
    MilitaryPersonnel self = new MilitaryPersonnel();
    self.setId(100L);
    self.setFullName("Self");
    self.setUnitCode("UNIT-A");

    MilitaryPersonnel sameUnitColleague = new MilitaryPersonnel();
    sameUnitColleague.setId(101L);
    sameUnitColleague.setFullName("Colleague");
    sameUnitColleague.setUnitCode("UNIT-A");

    MilitaryPersonnel otherUnit = new MilitaryPersonnel();
    otherUnit.setId(200L);
    otherUnit.setFullName("Outsider");
    otherUnit.setUnitCode("UNIT-B");

    User callerUser = new User();
    callerUser.setId(1L);
    callerUser.setMilitaryPersonnelId(100L);

    when(userRepository.findById(1L)).thenReturn(Optional.of(callerUser));
    when(militaryPersonnelRepository.findById(100L)).thenReturn(Optional.of(self));
    when(militaryPersonnelRepository.findAllList())
        .thenReturn(List.of(self, sameUnitColleague, otherUnit));

    authenticateAs(1L, "ROLE_USER");

    Pageable pageable = PageRequest.of(0, 10);
    Page<MilitaryPersonnelResponse> result = service.list(null, pageable);

    assertEquals(2, result.getTotalElements());
    assertEquals(
        List.of(100L, 101L),
        result.getContent().stream().map(MilitaryPersonnelResponse::getId).sorted().toList());
  }

  @Test
  @DisplayName("ROLE_USER getById() should be able to read a colleague from the same unit")
  void testRoleUserCanReadSameUnitColleagueById() {
    MilitaryPersonnel self = new MilitaryPersonnel();
    self.setId(100L);
    self.setFullName("Self");
    self.setUnitCode("UNIT-A");

    MilitaryPersonnel sameUnitColleague = new MilitaryPersonnel();
    sameUnitColleague.setId(101L);
    sameUnitColleague.setFullName("Colleague");
    sameUnitColleague.setUnitCode("UNIT-A");

    User callerUser = new User();
    callerUser.setId(1L);
    callerUser.setMilitaryPersonnelId(100L);

    when(userRepository.findById(1L)).thenReturn(Optional.of(callerUser));
    when(militaryPersonnelRepository.findById(100L)).thenReturn(Optional.of(self));
    when(militaryPersonnelRepository.findById(101L)).thenReturn(Optional.of(sameUnitColleague));

    authenticateAs(1L, "ROLE_USER");

    MilitaryPersonnelResponse response = service.getById(101L);

    assertEquals(101L, response.getId());
  }

  @Test
  @DisplayName("ROLE_USER getById() should still be denied for personnel in a different unit")
  void testRoleUserCannotReadOtherUnitPersonnelById() {
    MilitaryPersonnel self = new MilitaryPersonnel();
    self.setId(100L);
    self.setFullName("Self");
    self.setUnitCode("UNIT-A");

    MilitaryPersonnel otherUnit = new MilitaryPersonnel();
    otherUnit.setId(200L);
    otherUnit.setFullName("Outsider");
    otherUnit.setUnitCode("UNIT-B");

    User callerUser = new User();
    callerUser.setId(1L);
    callerUser.setMilitaryPersonnelId(100L);

    when(userRepository.findById(1L)).thenReturn(Optional.of(callerUser));
    when(militaryPersonnelRepository.findById(100L)).thenReturn(Optional.of(self));
    when(militaryPersonnelRepository.findById(200L)).thenReturn(Optional.of(otherUnit));

    authenticateAs(1L, "ROLE_USER");

    assertThrows(AppException.class, () -> service.getById(200L));
  }
}
