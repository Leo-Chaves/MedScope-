package com.cliniradar.repository;

import com.cliniradar.entity.SearchRequest;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SearchRequestRepository extends JpaRepository<SearchRequest, Long> {

    @Query(
            value = "SELECT cid_code, COUNT(*) AS total FROM search_request GROUP BY cid_code ORDER BY total DESC LIMIT 5",
            nativeQuery = true
    )
    List<Object[]> findTopCids();
}
