package com.cliniradar.service;

import com.cliniradar.dto.ArticleResponseDto;
import com.cliniradar.dto.SearchRequestDto;
import com.cliniradar.dto.SearchResponseDto;
import com.cliniradar.dto.SearchStreamEventDto;
import com.cliniradar.client.OllamaClient;
import com.cliniradar.entity.CidMapping;
import com.cliniradar.entity.SearchRequest;
import com.cliniradar.entity.User;
import com.cliniradar.repository.SearchRequestRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SearchService {

    private static final String DISCLAIMER =
            "Conteudo informativo para avaliacao profissional. Nao substitui o julgamento clinico.";
    private static final Pattern CID_PATTERN = Pattern.compile(
            "^([A-Z]\\d{1,2}(\\.\\d{1,2})?|\\d{1,2}[A-Z]\\d{1,2}(\\.\\d{1,2})?)$",
            Pattern.CASE_INSENSITIVE
    );

    private final CidMappingService cidMappingService;
    private final SearchRequestRepository searchRequestRepository;
    private final ArticleProcessingService articleProcessingService;
    private final CidArticleCacheService cidArticleCacheService;
    private final ScientificArticleSearchService scientificArticleSearchService;
    private final UserService userService;
    private final OllamaClient ollamaClient;

    public SearchService(CidMappingService cidMappingService,
                         SearchRequestRepository searchRequestRepository,
                         ArticleProcessingService articleProcessingService,
                         CidArticleCacheService cidArticleCacheService,
                         ScientificArticleSearchService scientificArticleSearchService,
                         UserService userService,
                         OllamaClient ollamaClient) {
        this.cidMappingService = cidMappingService;
        this.searchRequestRepository = searchRequestRepository;
        this.articleProcessingService = articleProcessingService;
        this.cidArticleCacheService = cidArticleCacheService;
        this.scientificArticleSearchService = scientificArticleSearchService;
        this.userService = userService;
        this.ollamaClient = ollamaClient;
    }

    @Transactional
    public SearchResponseDto search(SearchRequestDto requestDto) {
        QueryContext queryContext = resolveQueryContext(requestDto.getQuery());
        String sourceFilter = normalizeSourceFilter(requestDto.getSource());

        saveSearchRequest(queryContext.cidCode(), requestDto.getContext());

        if (queryContext.cidMapping() != null
                && !StringUtils.hasText(requestDto.getContext())
                && SearchRequestDto.SOURCE_PUBMED.equals(sourceFilter)) {
            return cidArticleCacheService.getCachedOrRefresh(
                    queryContext.cidMapping(),
                    DISCLAIMER,
                    requestDto.isContinueLoading()
            );
        }

        String queryUsed = buildQuery(queryContext.englishQueryBase(), requestDto.getContext());
        List<ArticleResponseDto> articles = scientificArticleSearchService.searchAcrossSources(
                        queryUsed,
                        sourceFilter
                ).stream()
                .map(articleProcessingService::saveAndAnalyzeArticle)
                .map(articleProcessingService::toResponse)
                .toList();

        return new SearchResponseDto(
                queryContext.cidCode(),
                queryContext.displayName(),
                queryUsed,
                null,
                DISCLAIMER,
                articles
        );
    }

    public void streamSearch(SearchRequestDto requestDto, Consumer<SearchStreamEventDto> eventSink) {
        QueryContext queryContext = resolveQueryContext(requestDto.getQuery());
        String sourceFilter = normalizeSourceFilter(requestDto.getSource());

        saveSearchRequest(queryContext.cidCode(), requestDto.getContext());

        if (queryContext.cidMapping() != null
                && !StringUtils.hasText(requestDto.getContext())
                && SearchRequestDto.SOURCE_PUBMED.equals(sourceFilter)) {
            SearchResponseDto cachedResponse = cidArticleCacheService.getCachedOrRefresh(
                    queryContext.cidMapping(),
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

        String queryUsed = buildQuery(queryContext.englishQueryBase(), requestDto.getContext());
        eventSink.accept(SearchStreamEventDto.meta(new SearchResponseDto(
                queryContext.cidCode(),
                queryContext.displayName(),
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

    private QueryContext resolveQueryContext(String rawQuery) {
        String trimmedQuery = rawQuery == null ? "" : rawQuery.trim();
        if (isCidQuery(trimmedQuery)) {
            String normalizedCid = cidMappingService.normalize(trimmedQuery);
            CidMapping mapping = cidMappingService.getByCode(normalizedCid);
            return new QueryContext(
                    normalizedCid,
                    mapping.getDisplayName(),
                    mapping.getEnglishQueryBase(),
                    mapping
            );
        }

        String translatedQuery = ollamaClient.translateMedicalTerm(trimmedQuery);
        return new QueryContext(
                trimmedQuery,
                trimmedQuery,
                translatedQuery,
                null
        );
    }

    private boolean isCidQuery(String value) {
        return StringUtils.hasText(value) && CID_PATTERN.matcher(value.trim()).matches();
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

    private record QueryContext(String cidCode, String displayName, String englishQueryBase, CidMapping cidMapping) {
    }

}
