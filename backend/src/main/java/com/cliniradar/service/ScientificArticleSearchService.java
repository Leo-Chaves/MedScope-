package com.cliniradar.service;

import com.cliniradar.client.ScientificArticleSearchClient;
import com.cliniradar.dto.SearchRequestDto;
import com.cliniradar.dto.ScientificArticleDto;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ScientificArticleSearchService {

    private final List<ScientificArticleSearchClient> clients;

    public ScientificArticleSearchService(List<ScientificArticleSearchClient> clients) {
        this.clients = clients;
    }

    public List<ScientificArticleDto> searchAcrossSources(String query) {
        return searchAcrossSources(query, SearchRequestDto.SOURCE_BOTH);
    }

    public List<ScientificArticleDto> searchAcrossSources(String query, String sourceFilter) {
        Map<String, ScientificArticleDto> deduplicated = new LinkedHashMap<>();
        String normalizedSourceFilter = normalizeSourceFilter(sourceFilter);

        for (ScientificArticleSearchClient client : clients) {
            for (ScientificArticleDto article : client.searchArticles(query)) {
                if (!matchesSourceFilter(article, normalizedSourceFilter)) {
                    continue;
                }
                deduplicated.putIfAbsent(normalizeKey(article), article);
            }
        }

        return deduplicated.values().stream()
                .sorted(Comparator.comparing(ScientificArticleDto::publishedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private String normalizeKey(ScientificArticleDto article) {
        return (article.source() + "|" + article.sourceId() + "|" + article.title()).toLowerCase();
    }

    private boolean matchesSourceFilter(ScientificArticleDto article, String sourceFilter) {
        return SearchRequestDto.SOURCE_BOTH.equals(sourceFilter) || sourceFilter.equalsIgnoreCase(article.source());
    }

    private String normalizeSourceFilter(String sourceFilter) {
        if (SearchRequestDto.SOURCE_PUBMED.equalsIgnoreCase(sourceFilter)) {
            return SearchRequestDto.SOURCE_PUBMED;
        }
        if (SearchRequestDto.SOURCE_SCIELO.equalsIgnoreCase(sourceFilter)) {
            return SearchRequestDto.SOURCE_SCIELO;
        }
        return SearchRequestDto.SOURCE_BOTH;
    }
}
