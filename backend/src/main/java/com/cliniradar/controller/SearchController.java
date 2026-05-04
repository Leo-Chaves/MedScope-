package com.cliniradar.controller;

import com.cliniradar.dto.SearchRequestDto;
import com.cliniradar.dto.SearchResponseDto;
import com.cliniradar.service.SearchService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @PostMapping
    public ResponseEntity<SearchResponseDto> search(@Valid @RequestBody SearchRequestDto requestDto) {
        return ResponseEntity.ok(searchService.search(requestDto));
    }
}
