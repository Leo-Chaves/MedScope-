package com.cliniradar.repository;

import com.cliniradar.entity.SearchRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SearchRequestRepository extends JpaRepository<SearchRequest, Long> {
}
