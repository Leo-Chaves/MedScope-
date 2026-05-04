package com.cliniradar.service;

import com.cliniradar.client.PubMedClient;
import com.cliniradar.dto.ArticleResponseDto;
import com.cliniradar.dto.SearchRequestDto;
import com.cliniradar.dto.SearchResponseDto;
import com.cliniradar.entity.CidMapping;
import com.cliniradar.entity.SearchRequest;
import com.cliniradar.repository.SearchRequestRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SearchService {

    private static final String DISCLAIMER =
            "Conteudo informativo para avaliacao profissional. Nao substitui o julgamento clinico.";

    private final CidMappingService cidMappingService;
    private final SearchRequestRepository searchRequestRepository;
    private final PubMedClient pubMedClient;
    private final ArticleProcessingService articleProcessingService;
    private final CidArticleCacheService cidArticleCacheService;

    public SearchService(CidMappingService cidMappingService,
                         SearchRequestRepository searchRequestRepository,
                         PubMedClient pubMedClient,
                         ArticleProcessingService articleProcessingService,
                         CidArticleCacheService cidArticleCacheService) {
        this.cidMappingService = cidMappingService;
        this.searchRequestRepository = searchRequestRepository;
        this.pubMedClient = pubMedClient;
        this.articleProcessingService = articleProcessingService;
        this.cidArticleCacheService = cidArticleCacheService;
    }

    @Transactional
    public SearchResponseDto search(SearchRequestDto requestDto) {
        String normalizedCid = cidMappingService.normalize(requestDto.getCid());
        CidMapping mapping = cidMappingService.getByCode(normalizedCid);

        searchRequestRepository.save(new SearchRequest(normalizedCid, requestDto.getContext()));

        if (!StringUtils.hasText(requestDto.getContext())) {
            return cidArticleCacheService.getCachedOrRefresh(mapping, DISCLAIMER, requestDto.isContinueLoading());
        }

        String queryUsed = buildQuery(mapping.getEnglishQueryBase(), requestDto.getContext());
        List<ArticleResponseDto> articles = pubMedClient.searchArticles(queryUsed).stream()
                .map(articleProcessingService::saveAndAnalyzeArticle)
                .map(articleProcessingService::toResponse)
                .filter(this::isDisplayableArticle)
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

    private String buildQuery(String base, String context) {
        if (!StringUtils.hasText(context)) {
            return base.trim();
        }
        return (base + " " + context.trim()).trim().replaceAll("\\s+", " ");
    }

    private boolean isDisplayableArticle(ArticleResponseDto article) {
        String warningNote = article.warningNote();
        return warningNote == null || !warningNote.toLowerCase().contains("fallback informacional");
    }
}
