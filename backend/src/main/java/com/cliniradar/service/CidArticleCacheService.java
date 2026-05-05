package com.cliniradar.service;

import com.cliniradar.client.PubMedClient;
import com.cliniradar.dto.ArticleResponseDto;
import com.cliniradar.dto.ScientificArticleDto;
import com.cliniradar.dto.SearchResponseDto;
import com.cliniradar.entity.ArticleSummary;
import com.cliniradar.entity.CidArticleCache;
import com.cliniradar.entity.CidMapping;
import com.cliniradar.repository.ArticleSummaryRepository;
import com.cliniradar.repository.CidArticleCacheRepository;
import com.cliniradar.repository.CidMappingRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class CidArticleCacheService {

    private static final Logger log = LoggerFactory.getLogger(CidArticleCacheService.class);

    private final CidMappingRepository cidMappingRepository;
    private final CidArticleCacheRepository cacheRepository;
    private final ArticleSummaryRepository articleSummaryRepository;
    private final PubMedClient pubMedClient;
    private final ArticleProcessingService articleProcessingService;

    public CidArticleCacheService(CidMappingRepository cidMappingRepository,
                                  CidArticleCacheRepository cacheRepository,
                                  ArticleSummaryRepository articleSummaryRepository,
                                  PubMedClient pubMedClient,
                                  ArticleProcessingService articleProcessingService) {
        this.cidMappingRepository = cidMappingRepository;
        this.cacheRepository = cacheRepository;
        this.articleSummaryRepository = articleSummaryRepository;
        this.pubMedClient = pubMedClient;
        this.articleProcessingService = articleProcessingService;
    }

    @Scheduled(
            fixedDelayString = "${summary-cache.refresh-interval-ms:43200000}",
            initialDelayString = "${summary-cache.initial-delay-ms:30000}"
    )
    public void refreshAllCidCaches() {
        List<CidMapping> mappings = cidMappingRepository.findAll();
        log.info("Atualizando cache de resumos para {} CIDs cadastrados.", mappings.size());

        for (CidMapping mapping : mappings) {
            try {
                refreshCidCache(mapping);
            } catch (RuntimeException ex) {
                log.warn("Falha ao atualizar cache do CID {}: {}", mapping.getCidCode(), ex.getMessage());
            }
        }
    }

    @Transactional
    public SearchResponseDto getCachedOrRefresh(CidMapping mapping, String disclaimer, boolean continueLoading) {
        List<CidArticleCache> entries = cacheRepository.findByCidCodeOrderByPositionAsc(mapping.getCidCode());
        if (!containsDisplayableSummary(entries)) {
            refreshUntilFirstDisplayable(mapping);
            entries = cacheRepository.findByCidCodeOrderByPositionAsc(mapping.getCidCode());
        } else if (continueLoading && hasPendingOrFallbackSummary(entries)) {
            refreshNextPendingOrFallback(mapping);
            entries = cacheRepository.findByCidCodeOrderByPositionAsc(mapping.getCidCode());
        }
        return toSearchResponse(mapping, entries, disclaimer);
    }

    @Transactional
    public void refreshCidCache(CidMapping mapping) {
        String cidCode = mapping.getCidCode();
        String queryUsed = mapping.getEnglishQueryBase().trim();
        List<ScientificArticleDto> articles = pubMedClient.searchArticles(queryUsed);

        if (articles.isEmpty()) {
            log.info("PubMed nao retornou artigos para o CID {}. Cache existente preservado.", cidCode);
            return;
        }

        LocalDateTime refreshedAt = LocalDateTime.now();
        for (int index = 0; index < articles.size(); index++) {
            int position = index + 1;
            ArticleSummary summary = articleProcessingService.saveAndAnalyzeArticle(articles.get(index));
            CidArticleCache cache = cacheRepository.findByCidCodeAndPosition(cidCode, position)
                    .orElseGet(() -> new CidArticleCache(cidCode, position));

            cache.setArticle(summary.getArticle());
            cache.setQueryUsed(queryUsed);
            cache.setRefreshedAt(refreshedAt);
            cacheRepository.save(cache);
        }

        List<CidArticleCache> existing = cacheRepository.findByCidCodeOrderByPositionAsc(cidCode);
        List<CidArticleCache> stale = existing.stream()
                .filter(cache -> cache.getPosition() > articles.size())
                .toList();
        cacheRepository.deleteAll(stale);
    }

    @Transactional
    public void refreshUntilFirstDisplayable(CidMapping mapping) {
        refreshProgressively(mapping, true);
    }

    @Transactional
    public void refreshNextPendingOrFallback(CidMapping mapping) {
        refreshProgressively(mapping, false);
    }

    private void refreshProgressively(CidMapping mapping, boolean waitForDisplayable) {
        String cidCode = mapping.getCidCode();
        String queryUsed = mapping.getEnglishQueryBase().trim();
        List<ScientificArticleDto> articles = pubMedClient.searchArticles(queryUsed);

        if (articles.isEmpty()) {
            log.info("PubMed nao retornou artigos para o CID {}. Cache existente preservado.", cidCode);
            return;
        }

        LocalDateTime refreshedAt = LocalDateTime.now();
        for (int index = 0; index < articles.size(); index++) {
            int position = index + 1;
            CidArticleCache existingCache = cacheRepository.findByCidCodeAndPosition(cidCode, position).orElse(null);
            if (existingCache != null && hasDisplayableSummary(existingCache)) {
                continue;
            }

            ArticleSummary summary = articleProcessingService.saveAndAnalyzeArticle(articles.get(index));
            CidArticleCache cache = existingCache != null ? existingCache : new CidArticleCache(cidCode, position);
            cache.setArticle(summary.getArticle());
            cache.setQueryUsed(queryUsed);
            cache.setRefreshedAt(refreshedAt);
            cacheRepository.save(cache);

            if (!waitForDisplayable || !isFallbackSummary(summary)) {
                return;
            }
        }
    }

    private SearchResponseDto toSearchResponse(CidMapping mapping,
                                               List<CidArticleCache> entries,
                                               String disclaimer) {
        String queryUsed = entries.stream()
                .findFirst()
                .map(CidArticleCache::getQueryUsed)
                .orElse(mapping.getEnglishQueryBase().trim());

        LocalDateTime refreshedAt = entries.stream()
                .map(CidArticleCache::getRefreshedAt)
                .filter(value -> value != null)
                .max(Comparator.naturalOrder())
                .orElse(null);

        List<ArticleResponseDto> articles = entries.stream()
                .map(cache -> articleSummaryRepository.findByArticle(cache.getArticle()))
                .flatMap(Optional::stream)
                .map(articleProcessingService::toResponse)
                .toList();

        return new SearchResponseDto(
                mapping.getCidCode(),
                mapping.getDisplayName(),
                queryUsed,
                refreshedAt,
                disclaimer,
                articles
        );
    }

    private boolean containsDisplayableSummary(List<CidArticleCache> entries) {
        return entries.stream()
                .map(cache -> articleSummaryRepository.findByArticle(cache.getArticle()))
                .flatMap(Optional::stream)
                .anyMatch(summary -> !isFallbackSummary(summary));
    }

    private boolean hasPendingOrFallbackSummary(List<CidArticleCache> entries) {
        if (entries.size() < 2) {
            return true;
        }

        return entries.stream().anyMatch(cache -> !hasDisplayableSummary(cache));
    }

    private boolean hasDisplayableSummary(CidArticleCache cache) {
        return articleSummaryRepository.findByArticle(cache.getArticle())
                .filter(summary -> !isFallbackSummary(summary))
                .isPresent();
    }

    private boolean isFallbackSummary(ArticleSummary summary) {
        String warningNote = summary.getWarningNote();
        return warningNote != null && warningNote.toLowerCase().contains("fallback informacional");
    }
}
