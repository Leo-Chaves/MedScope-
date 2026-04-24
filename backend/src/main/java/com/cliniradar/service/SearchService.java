package com.cliniradar.service;

import com.cliniradar.client.OllamaClient;
import com.cliniradar.client.PubMedClient;
import com.cliniradar.dto.ArticleResponseDto;
import com.cliniradar.dto.OllamaAnalysisDto;
import com.cliniradar.dto.PubMedArticleDto;
import com.cliniradar.dto.SearchRequestDto;
import com.cliniradar.dto.SearchResponseDto;
import com.cliniradar.entity.Article;
import com.cliniradar.entity.ArticleSummary;
import com.cliniradar.entity.CidMapping;
import com.cliniradar.entity.SearchRequest;
import com.cliniradar.exception.ExternalServiceException;
import com.cliniradar.repository.ArticleRepository;
import com.cliniradar.repository.ArticleSummaryRepository;
import com.cliniradar.repository.SearchRequestRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SearchService {

    private static final String DISCLAIMER =
            "Conteúdo informativo para avaliação profissional. Não substitui o julgamento clínico.";
    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    private final CidMappingService cidMappingService;
    private final SearchRequestRepository searchRequestRepository;
    private final ArticleRepository articleRepository;
    private final ArticleSummaryRepository articleSummaryRepository;
    private final PubMedClient pubMedClient;
    private final OllamaClient ollamaClient;
    private final PromptBuilderService promptBuilderService;

    public SearchService(CidMappingService cidMappingService,
                         SearchRequestRepository searchRequestRepository,
                         ArticleRepository articleRepository,
                         ArticleSummaryRepository articleSummaryRepository,
                         PubMedClient pubMedClient,
                         OllamaClient ollamaClient,
                         PromptBuilderService promptBuilderService) {
        this.cidMappingService = cidMappingService;
        this.searchRequestRepository = searchRequestRepository;
        this.articleRepository = articleRepository;
        this.articleSummaryRepository = articleSummaryRepository;
        this.pubMedClient = pubMedClient;
        this.ollamaClient = ollamaClient;
        this.promptBuilderService = promptBuilderService;
    }

    @Transactional
    public SearchResponseDto search(SearchRequestDto requestDto) {
        String normalizedCid = cidMappingService.normalize(requestDto.getCid());
        CidMapping mapping = cidMappingService.getByCode(normalizedCid);
        String queryUsed = buildQuery(mapping.getEnglishQueryBase(), requestDto.getContext());

        searchRequestRepository.save(new SearchRequest(normalizedCid, requestDto.getContext()));

        List<ArticleResponseDto> articles = pubMedClient.searchArticles(queryUsed).stream()
                .map(this::saveAndAnalyzeArticle)
                .map(this::toResponse)
                .toList();

        return new SearchResponseDto(
                normalizedCid,
                mapping.getDisplayName(),
                queryUsed,
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

    private ArticleSummary saveAndAnalyzeArticle(PubMedArticleDto dto) {
        Article article = articleRepository.findByPubmedId(dto.pubmedId())
                .map(existing -> updateArticle(existing, dto))
                .orElseGet(() -> articleRepository.save(new Article(
                        dto.pubmedId(),
                        dto.title(),
                        dto.abstractText(),
                        dto.journal(),
                        dto.publishedAt(),
                        dto.publicationType(),
                        dto.url()
                )));

        ArticleSummary existingSummary = articleSummaryRepository.findByArticle(article).orElse(null);
        if (canReuseSummary(existingSummary)) {
            return existingSummary;
        }

        OllamaAnalysisDto analysis;
        try {
            analysis = ollamaClient.analyzeArticle(promptBuilderService.buildArticleAnalysisPrompt(dto));
        } catch (ExternalServiceException ex) {
            log.warn("Fallback acionado para o artigo PubMed {}: {}", dto.pubmedId(), ex.getMessage());
            analysis = buildFallbackAnalysis(dto, ex.getMessage());
        }

        ArticleSummary summary = existingSummary != null ? existingSummary : new ArticleSummary(article);

        summary.setSummaryPt(defaultValue(
                analysis.getSummaryPt(),
                "Resumo informativo indisponível no momento. Verifique o artigo original para apoio profissional."
        ));
        summary.setEvidenceType(defaultValue(analysis.getEvidenceType(), "Tipo de evidência não identificado."));
        summary.setRelevanceLevel(normalizeRelevance(analysis.getRelevanceLevel()));
        summary.setPracticalImpact(defaultValue(
                analysis.getPracticalImpact(),
                "Impacto prático incerto; a interpretação deve ser cautelosa e contextualizada."
        ));
        summary.setWarningNote(defaultValue(
                analysis.getWarningNote(),
                "Material de apoio informacional, sem recomendação de conduta ou tratamento."
        ));

        return articleSummaryRepository.save(summary);
    }

    private Article updateArticle(Article article, PubMedArticleDto dto) {
        article.setTitle(dto.title());
        article.setAbstractText(dto.abstractText());
        article.setJournal(dto.journal());
        article.setPublishedAt(dto.publishedAt());
        article.setPublicationType(dto.publicationType());
        article.setUrl(dto.url());
        return articleRepository.save(article);
    }

    private boolean canReuseSummary(ArticleSummary summary) {
        if (summary == null) {
            return false;
        }

        if (!StringUtils.hasText(summary.getSummaryPt())) {
            return false;
        }

        String warningNote = summary.getWarningNote();
        return !StringUtils.hasText(warningNote)
                || !warningNote.toLowerCase().contains("fallback informacional");
    }

    private OllamaAnalysisDto buildFallbackAnalysis(PubMedArticleDto dto, String reason) {
        OllamaAnalysisDto fallback = new OllamaAnalysisDto();
        fallback.setSummaryPt(
                "Análise automática indisponível no momento. Consulte o título, o resumo e o artigo original para avaliação profissional."
        );
        fallback.setEvidenceType(StringUtils.hasText(dto.publicationType())
                ? dto.publicationType()
                : "Tipo de evidência não identificado.");
        fallback.setRelevanceLevel("BAIXO");
        fallback.setPracticalImpact(
                "Impacto prático não classificado automaticamente devido à indisponibilidade ou à lentidão do modelo local."
        );
        fallback.setWarningNote(
                "Resultado com fallback informacional. Motivo: " + reason
        );
        return fallback;
    }

    private ArticleResponseDto toResponse(ArticleSummary summary) {
        Article article = summary.getArticle();
        return new ArticleResponseDto(
                article.getPubmedId(),
                article.getTitle(),
                article.getPublishedAt(),
                article.getPublicationType(),
                article.getJournal(),
                article.getUrl(),
                summary.getSummaryPt(),
                summary.getRelevanceLevel(),
                summary.getEvidenceType(),
                summary.getPracticalImpact(),
                summary.getWarningNote()
        );
    }

    private String normalizeRelevance(String value) {
        if (!StringUtils.hasText(value)) {
            return "BAIXO";
        }

        String normalized = value.trim().toUpperCase();
        if (normalized.contains("ALTO")) {
            return "ALTO";
        }
        if (normalized.contains("MED")) {
            return "MEDIO";
        }
        return "BAIXO";
    }

    private String defaultValue(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }
}
