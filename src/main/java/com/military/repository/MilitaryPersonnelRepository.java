package com.military.repository;

import com.military.models.MilitaryPersonnel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MilitaryPersonnelRepository extends JpaRepository<MilitaryPersonnel, Long> {
  Optional<MilitaryPersonnel> findFirstByCodeStartingWithOrderByCodeDesc(String prefix);

  boolean existsByCode(String code);

  Page<MilitaryPersonnel> findByFullNameContainingIgnoreCaseOrCodeContainingIgnoreCase(String fullNameKeyword,
                                                                                                 String codeKeyword,
                                                                                                 Pageable pageable);
}
