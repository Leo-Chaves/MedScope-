package com.cliniradar.service;

import com.cliniradar.dto.ArticleResponseDto;
import com.cliniradar.dto.SearchRequestDto;
import com.cliniradar.dto.SearchResponseDto;
import com.cliniradar.dto.SearchStreamEventDto;
import com.cliniradar.entity.CidMapping;
import com.cliniradar.entity.SearchRequest;
import com.cliniradar.entity.User;
import com.cliniradar.repository.SearchRequestRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SearchService {

    private static final String DISCLAIMER =
            "Conteudo informativo para avaliacao profissional. Nao substitui o julgamento clinico.";

    private final CidMappingService cidMappingService;
    private final SearchRequestRepository searchRequestRepository;
    private final ArticleProcessingService articleProcessingService;
    private final CidArticleCacheService cidArticleCacheService;
    private final ScientificArticleSearchService scientificArticleSearchService;
    private final UserService userService;

    public SearchService(CidMappingService cidMappingService,
                         SearchRequestRepository searchRequestRepository,
                         ArticleProcessingService articleProcessingService,
                         CidArticleCacheService cidArticleCacheService,
                         ScientificArticleSearchService scientificArticleSearchService,
                         UserService userService) {
        this.cidMappingService = cidMappingService;
        this.searchRequestRepository = searchRequestRepository;
        this.articleProcessingService = articleProcessingService;
        this.cidArticleCacheService = cidArticleCacheService;
        this.scientificArticleSearchService = scientificArticleSearchService;
        this.userService = userService;
    }

    @Transactional
    public SearchResponseDto search(SearchRequestDto requestDto) {
        String normalizedCid = cidMappingService.normalize(requestDto.getCid());
        CidMapping mapping = cidMappingService.getByCode(normalizedCid);
        String sourceFilter = normalizeSourceFilter(requestDto.getSource());

        saveSearchRequest(normalizedCid, requestDto.getContext());

        if (!StringUtils.hasText(requestDto.getContext()) && SearchRequestDto.SOURCE_PUBMED.equals(sourceFilter)) {
            return cidArticleCacheService.getCachedOrRefresh(mapping, DISCLAIMER, requestDto.isContinueLoading());
        }

        String queryUsed = buildQuery(mapping.getEnglishQueryBase(), requestDto.getContext());
        List<ArticleResponseDto> articles = scientificArticleSearchService.searchAcrossSources(
                        queryUsed,
                        sourceFilter
                ).stream()
                .map(articleProcessingService::saveAndAnalyzeArticle)
                .map(articleProcessingService::toResponse)
                .toList();

        return new SearchResponseDto(
                normalizedCid,
                mapping.getDisplayName(),
                queryUsed,
                null,
                DISCLAIMER,
                articles
        );
    }

    public void streamSearch(SearchRequestDto requestDto, Consumer<SearchStreamEventDto> eventSink) {
        String normalizedCid = cidMappingService.normalize(requestDto.getCid());
        CidMapping mapping = cidMappingService.getByCode(normalizedCid);
        String sourceFilter = normalizeSourceFilter(requestDto.getSource());

        saveSearchRequest(normalizedCid, requestDto.getContext());

        if (!StringUtils.hasText(requestDto.getContext()) && SearchRequestDto.SOURCE_PUBMED.equals(sourceFilter)) {
            SearchResponseDto cachedResponse = cidArticleCacheService.getCachedOrRefresh(
                    mapping,
                    DISCLAIMER,
                    requestDto.isContinueLoading()
            );
            eventSink.accept(SearchStreamEventDto.meta(new SearchResponseDto(
                    cachedResponse.cid(),
                    cachedResponse.condition(),
                    cachedResponse.queryUsed(),
                    cachedResponse.refreshedAt(),
                    cachedResponse.disclaimer(),
                    List.of()
            )));
            for (ArticleResponseDto article : cachedResponse.articles()) {
                eventSink.accept(SearchStreamEventDto.article(article));
            }
            eventSink.accept(SearchStreamEventDto.complete());
            return;
        }

        String queryUsed = buildQuery(mapping.getEnglishQueryBase(), requestDto.getContext());
        eventSink.accept(SearchStreamEventDto.meta(new SearchResponseDto(
                normalizedCid,
                mapping.getDisplayName(),
                queryUsed,
                null,
                DISCLAIMER,
                List.of()
        )));

        for (var article : scientificArticleSearchService.searchAcrossSources(queryUsed, sourceFilter)) {
            ArticleResponseDto response = articleProcessingService.toResponse(
                    articleProcessingService.saveAndAnalyzeArticle(article)
            );
            eventSink.accept(SearchStreamEventDto.article(response));
        }

        eventSink.accept(SearchStreamEventDto.complete());
    }

    private String buildQuery(String base, String context) {
        if (!StringUtils.hasText(context)) {
            return base.trim();
        }
        return (base + " " + context.trim()).trim().replaceAll("\\s+", " ");
    }

    private String normalizeSourceFilter(String source) {
        if (SearchRequestDto.SOURCE_PUBMED.equalsIgnoreCase(source)) {
            return SearchRequestDto.SOURCE_PUBMED;
        }
        if (SearchRequestDto.SOURCE_SCIELO.equalsIgnoreCase(source)) {
            return SearchRequestDto.SOURCE_SCIELO;
        }
        return SearchRequestDto.SOURCE_BOTH;
    }

    private void saveSearchRequest(String cidCode, String context) {
        SearchRequest searchRequest = new SearchRequest(cidCode, context);
        resolveAuthenticatedUser().ifPresent(searchRequest::setUser);
        searchRequestRepository.save(searchRequest);
    }

    private Optional<User> resolveAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return userService.findByEmail(authentication.getName());
    }

}
