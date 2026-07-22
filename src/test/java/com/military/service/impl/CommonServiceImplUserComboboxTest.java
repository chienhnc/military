package com.military.service.impl;

import com.military.models.MilitaryPersonnel;
import com.military.models.User;
import com.military.payload.response.ComboboxOptionResponse;
import com.military.repository.MilitaryPersonnelRepository;
import com.military.repository.MilitaryUnitRepository;
import com.military.repository.UserRepository;
import com.military.service.MilitaryPersonnelService;
import com.military.service.MilitaryUnitService;
import com.military.service.VehicleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommonServiceImpl.getUserCombobox() Tests")
class CommonServiceImplUserComboboxTest {

  @Mock
  private MilitaryPersonnelService militaryPersonnelService;

  @Mock
  private MilitaryUnitService militaryUnitService;

  @Mock
  private VehicleService vehicleService;

  @Mock
  private UserRepository userRepository;

  @Mock
  private MilitaryPersonnelRepository militaryPersonnelRepository;

  @Mock
  private MilitaryUnitRepository militaryUnitRepository;

  private CommonServiceImpl commonService;

  @BeforeEach
  void setUp() {
    commonService = new CommonServiceImpl(
        militaryPersonnelService,
        militaryUnitService,
        vehicleService,
        userRepository,
        militaryPersonnelRepository,
        militaryUnitRepository
    );
  }

  @Test
  @DisplayName("Should include user with existing militaryPersonnelId")
  void testIncludeUserWithExistingPersonnel() {
    // Arrange
    User user1 = new User();
    user1.setId(1L);
    user1.setUsername("john_doe");
    user1.setMilitaryPersonnelId(100L);

    MilitaryPersonnel personnel1 = new MilitaryPersonnel();
    personnel1.setId(100L);
    personnel1.setFullName("John Doe");

    when(userRepository.findAllList()).thenReturn(List.of(user1));
    when(militaryPersonnelRepository.findAllList()).thenReturn(List.of(personnel1));

    // Act
    List<ComboboxOptionResponse> result = commonService.getUserCombobox();

    // Assert
    assertEquals(1, result.size());
    ComboboxOptionResponse response = result.get(0);
    assertEquals("1", response.code());
    assertEquals("John Doe (john_doe)", response.name());
  }

  @Test
  @DisplayName("Should exclude user with null militaryPersonnelId")
  void testExcludeUserWithNullPersonnelId() {
    // Arrange
    User user1 = new User();
    user1.setId(1L);
    user1.setUsername("jane_smith");
    user1.setMilitaryPersonnelId(null);

    User user2 = new User();
    user2.setId(2L);
    user2.setUsername("john_doe");
    user2.setMilitaryPersonnelId(100L);

    MilitaryPersonnel personnel2 = new MilitaryPersonnel();
    personnel2.setId(100L);
    personnel2.setFullName("John Doe");

    when(userRepository.findAllList()).thenReturn(List.of(user1, user2));
    when(militaryPersonnelRepository.findAllList()).thenReturn(List.of(personnel2));

    // Act
    List<ComboboxOptionResponse> result = commonService.getUserCombobox();

    // Assert
    assertEquals(1, result.size());
    assertEquals("2", result.get(0).code());
  }

  @Test
  @DisplayName("Should exclude user with non-existent militaryPersonnelId")
  void testExcludeUserWithNonExistentPersonnelId() {
    // Arrange
    User user1 = new User();
    user1.setId(1L);
    user1.setUsername("john_doe");
    user1.setMilitaryPersonnelId(100L);

    User user2 = new User();
    user2.setId(2L);
    user2.setUsername("jane_smith");
    user2.setMilitaryPersonnelId(999L); // Non-existent personnel

    MilitaryPersonnel personnel1 = new MilitaryPersonnel();
    personnel1.setId(100L);
    personnel1.setFullName("John Doe");

    when(userRepository.findAllList()).thenReturn(List.of(user1, user2));
    when(militaryPersonnelRepository.findAllList()).thenReturn(List.of(personnel1));

    // Act
    List<ComboboxOptionResponse> result = commonService.getUserCombobox();

    // Assert
    assertEquals(1, result.size());
    assertEquals("1", result.get(0).code());
  }

  @Test
  @DisplayName("Should return empty list when user list is empty")
  void testEmptyUserList() {
    // Arrange
    MilitaryPersonnel personnel1 = new MilitaryPersonnel();
    personnel1.setId(100L);
    personnel1.setFullName("John Doe");

    when(userRepository.findAllList()).thenReturn(List.of());
    when(militaryPersonnelRepository.findAllList()).thenReturn(List.of(personnel1));

    // Act
    List<ComboboxOptionResponse> result = commonService.getUserCombobox();

    // Assert
    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("Should return empty list when personnel list is empty")
  void testEmptyPersonnelList() {
    // Arrange
    User user1 = new User();
    user1.setId(1L);
    user1.setUsername("john_doe");
    user1.setMilitaryPersonnelId(100L);

    when(userRepository.findAllList()).thenReturn(List.of(user1));
    when(militaryPersonnelRepository.findAllList()).thenReturn(List.of());

    // Act
    List<ComboboxOptionResponse> result = commonService.getUserCombobox();

    // Assert
    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("Should handle multiple users and personnel correctly")
  void testMultipleUsersAndPersonnel() {
    // Arrange
    User user1 = new User();
    user1.setId(1L);
    user1.setUsername("john_doe");
    user1.setMilitaryPersonnelId(100L);

    User user2 = new User();
    user2.setId(2L);
    user2.setUsername("jane_smith");
    user2.setMilitaryPersonnelId(200L);

    User user3 = new User();
    user3.setId(3L);
    user3.setUsername("bob_wilson");
    user3.setMilitaryPersonnelId(999L); // Non-existent

    User user4 = new User();
    user4.setId(4L);
    user4.setUsername("alice_brown");
    user4.setMilitaryPersonnelId(null); // Null

    MilitaryPersonnel personnel1 = new MilitaryPersonnel();
    personnel1.setId(100L);
    personnel1.setFullName("John Doe");

    MilitaryPersonnel personnel2 = new MilitaryPersonnel();
    personnel2.setId(200L);
    personnel2.setFullName("Jane Smith");

    when(userRepository.findAllList()).thenReturn(List.of(user1, user2, user3, user4));
    when(militaryPersonnelRepository.findAllList()).thenReturn(List.of(personnel1, personnel2));

    // Act
    List<ComboboxOptionResponse> result = commonService.getUserCombobox();

    // Assert
    assertEquals(2, result.size());
    assertTrue(result.stream().anyMatch(r -> r.code().equals("1") && r.name().equals("John Doe (john_doe)")));
    assertTrue(result.stream().anyMatch(r -> r.code().equals("2") && r.name().equals("Jane Smith (jane_smith)")));
  }

  @Test
  @DisplayName("Should not throw exception with empty lists")
  void testNoExceptionWithEmptyLists() {
    // Arrange
    when(userRepository.findAllList()).thenReturn(List.of());
    when(militaryPersonnelRepository.findAllList()).thenReturn(List.of());

    // Act & Assert - should not throw
    assertDoesNotThrow(() -> commonService.getUserCombobox());
    List<ComboboxOptionResponse> result = commonService.getUserCombobox();
    assertTrue(result.isEmpty());
  }
}
