package com.military.repository;

import com.military.models.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository {
  Vehicle save(Vehicle vehicle);

  /**
   * Atomically creates a vehicle for a personnel who doesn't have one yet.
   * Fails with {@code ConditionalCheckFailedException} if the personnel already has a vehicle
   * (enforces the 1:1 invariant at the DynamoDB layer, closing the check-then-act race that a
   * plain findByPersonnelId()-then-save() would have under concurrent requests).
   */
  Vehicle createForPersonnel(Vehicle vehicle);

  Optional<Vehicle> findById(Long id);

  Optional<Vehicle> findByPersonnelId(Long personnelId);

  Page<Vehicle> findAll(Pageable pageable);

  void delete(Vehicle vehicle);

  List<Vehicle> findAllList();

  Page<Vehicle> findByLicensePlateContainingIgnoreCaseOrBrandContainingIgnoreCaseOrModelContainingIgnoreCase(
      String plateKeyword, String brandKeyword, String modelKeyword, Pageable pageable);
}
