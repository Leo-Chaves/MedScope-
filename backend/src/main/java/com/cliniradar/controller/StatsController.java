package com.cliniradar.controller;

import com.cliniradar.dto.TopCidDto;
import com.cliniradar.repository.SearchRequestRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final SearchRequestRepository searchRequestRepository;

    public StatsController(SearchRequestRepository searchRequestRepository) {
        this.searchRequestRepository = searchRequestRepository;
    }

    @GetMapping("/top-cids")
    public List<TopCidDto> topCids() {
        return searchRequestRepository.findTopCids().stream()
                .map(row -> new TopCidDto((String) row[0], ((Number) row[1]).longValue()))
                .toList();
    }
}
