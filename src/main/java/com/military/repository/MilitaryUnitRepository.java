package com.military.repository;

import com.military.models.MilitaryUnit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MilitaryUnitRepository {
  MilitaryUnit save(MilitaryUnit unit);

  Optional<MilitaryUnit> findById(Long id);

  Page<MilitaryUnit> findAll(Pageable pageable);

  void delete(MilitaryUnit unit);

  boolean existsByUnitCode(String unitCode);

  Optional<MilitaryUnit> findByUnitCodeIgnoreCase(String unitCode);

  List<MilitaryUnit> findAllList();

  List<MilitaryUnit> findByRegionCodeIgnoreCase(String regionCode);

  Page<MilitaryUnit> findByUnitCodeContainingIgnoreCaseOrUnitNameContainingIgnoreCase(String unitCodeKeyword,
                                                                                        String unitNameKeyword,
                                                                                        Pageable pageable);
}
