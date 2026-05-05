package com.cliniradar.controller;

import com.cliniradar.dto.SearchRequestDto;
import com.cliniradar.dto.SearchResponseDto;
import com.cliniradar.dto.SearchStreamEventDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cliniradar.service.SearchService;
import java.nio.charset.StandardCharsets;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;
    private final ObjectMapper objectMapper;

    public SearchController(SearchService searchService, ObjectMapper objectMapper) {
        this.searchService = searchService;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<SearchResponseDto> search(@Valid @RequestBody SearchRequestDto requestDto) {
        return ResponseEntity.ok(searchService.search(requestDto));
    }

    @PostMapping(value = "/stream", produces = "application/x-ndjson")
    public ResponseEntity<StreamingResponseBody> stream(@Valid @RequestBody SearchRequestDto requestDto) {
        StreamingResponseBody responseBody = outputStream -> {
            try {
                searchService.streamSearch(requestDto, event -> writeEvent(outputStream, event));
            } catch (Exception ex) {
                writeEvent(outputStream, SearchStreamEventDto.error(ex.getMessage()));
            }
        };

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/x-ndjson"))
                .body(responseBody);
    }

    private void writeEvent(java.io.OutputStream outputStream, SearchStreamEventDto event) {
        try {
            outputStream.write(objectMapper.writeValueAsString(event).getBytes(StandardCharsets.UTF_8));
            outputStream.write('\n');
            outputStream.flush();
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao transmitir evento de busca.", ex);
        }
    }
}
