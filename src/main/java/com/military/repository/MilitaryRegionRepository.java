package com.military.repository;

import com.military.models.MilitaryRegion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MilitaryRegionRepository {
  MilitaryRegion save(MilitaryRegion region);

  Optional<MilitaryRegion> findById(Long id);

  Page<MilitaryRegion> findAll(Pageable pageable);

  void delete(MilitaryRegion region);

  boolean existsByRegionCode(String regionCode);

  Optional<MilitaryRegion> findByRegionCodeIgnoreCase(String regionCode);

  List<MilitaryRegion> findAllList();

  Page<MilitaryRegion> findByRegionCodeContainingIgnoreCaseOrRegionNameContainingIgnoreCase(String regionCodeKeyword,
                                                                                              String regionNameKeyword,
                                                                                              Pageable pageable);
}
