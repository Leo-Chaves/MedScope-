package com.cliniradar.repository;

import com.cliniradar.entity.CidMapping;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CidMappingRepository extends JpaRepository<CidMapping, Long> {

    Optional<CidMapping> findByCidCodeIgnoreCase(String cidCode);
}
