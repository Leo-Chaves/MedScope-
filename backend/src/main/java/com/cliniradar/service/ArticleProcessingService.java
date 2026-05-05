package com.cliniradar.service;

import com.cliniradar.client.OllamaClient;
import com.cliniradar.dto.ArticleResponseDto;
import com.cliniradar.dto.OllamaAnalysisDto;
import com.cliniradar.dto.ScientificArticleDto;
import com.cliniradar.entity.Article;
import com.cliniradar.entity.ArticleSummary;
import com.cliniradar.exception.ExternalServiceException;
import com.cliniradar.repository.ArticleRepository;
import com.cliniradar.repository.ArticleSummaryRepository;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ArticleProcessingService {

    private static final Logger log = LoggerFactory.getLogger(ArticleProcessingService.class);

    private final ArticleRepository articleRepository;
    private final ArticleSummaryRepository articleSummaryRepository;
    private final OllamaClient ollamaClient;
    private final PromptBuilderService promptBuilderService;

    public ArticleProcessingService(ArticleRepository articleRepository,
                                    ArticleSummaryRepository articleSummaryRepository,
                                    OllamaClient ollamaClient,
                                    PromptBuilderService promptBuilderService) {
        this.articleRepository = articleRepository;
        this.articleSummaryRepository = articleSummaryRepository;
        this.ollamaClient = ollamaClient;
        this.promptBuilderService = promptBuilderService;
    }

    @Transactional
    public ArticleSummary saveAndAnalyzeArticle(ScientificArticleDto dto) {
        String newHash = contentHash(dto);
        Article existingArticle = articleRepository.findByPubmedId(articleKey(dto)).orElse(null);
        boolean articleChanged = existingArticle != null && hasArticleChanged(existingArticle, dto, newHash);
        Article article = existingArticle != null
                ? updateArticle(existingArticle, dto, newHash)
                : createArticle(dto, newHash);

        ArticleSummary existingSummary = articleSummaryRepository.findByArticle(article).orElse(null);
        if (!articleChanged && canReuseSummary(existingSummary)) {
            return initializeArticle(existingSummary);
        }

        OllamaAnalysisDto analysis;
        try {
            analysis = ollamaClient.analyzeArticle(promptBuilderService.buildArticleAnalysisPrompt(dto));
        } catch (ExternalServiceException ex) {
            log.warn("Fallback acionado para o artigo {} {}: {}", dto.source(), dto.sourceId(), ex.getMessage());
            analysis = buildFallbackAnalysis(dto, ex.getMessage());
        }

        ArticleSummary summary = existingSummary != null ? existingSummary : new ArticleSummary(article);

        summary.setSummaryPt(defaultValue(
                analysis.getSummaryPt(),
                "Resumo informativo indisponivel no momento. Verifique o artigo original para apoio profissional."
        ));
        summary.setEvidenceType(defaultValue(analysis.getEvidenceType(), "Tipo de evidencia nao identificado."));
        summary.setRelevanceLevel(normalizeRelevance(analysis.getRelevanceLevel()));
        summary.setPracticalImpact(defaultValue(
                analysis.getPracticalImpact(),
                "Impacto pratico incerto; a interpretacao deve ser cautelosa e contextualizada."
        ));
        summary.setWarningNote(defaultValue(
                analysis.getWarningNote(),
                "Material de apoio informacional, sem recomendacao de conduta ou tratamento."
        ));

        return initializeArticle(articleSummaryRepository.save(summary));
    }

    public ArticleResponseDto toResponse(ArticleSummary summary) {
        Article article = summary.getArticle();
        String sourceKey = article.getPubmedId();
        return new ArticleResponseDto(
                resolveSource(sourceKey),
                resolveSourceId(sourceKey),
                article.getTitle(),
                displayablePublishedAt(article.getPublishedAt(), article.getPublicationDateDisplay()),
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

    private String displayablePublishedAt(LocalDate publishedAt, String publicationDateDisplay) {
        if (publishedAt != null && publishedAt.isAfter(LocalDate.now())) {
            return null;
        }
        if (StringUtils.hasText(publicationDateDisplay)) {
            return publicationDateDisplay;
        }
        return publishedAt != null ? publishedAt.toString() : null;
    }

    private Article createArticle(ScientificArticleDto dto, String contentHash) {
        Article article = new Article(
                articleKey(dto),
                dto.title(),
                dto.abstractText(),
                dto.journal(),
                dto.publishedAt(),
                dto.publishedAtDisplay(),
                dto.publicationType(),
                dto.url()
        );
        article.setContentHash(contentHash);
        return articleRepository.save(article);
    }

    private ArticleSummary initializeArticle(ArticleSummary summary) {
        summary.getArticle().getId();
        return summary;
    }

    private Article updateArticle(Article article, ScientificArticleDto dto, String newHash) {
        article.setTitle(dto.title());
        article.setAbstractText(dto.abstractText());
        article.setJournal(dto.journal());
        article.setPublishedAt(dto.publishedAt());
        article.setPublicationDateDisplay(dto.publishedAtDisplay());
        article.setPublicationType(dto.publicationType());
        article.setUrl(dto.url());
        article.setContentHash(newHash);
        return articleRepository.save(article);
    }

    private boolean hasArticleChanged(Article article, ScientificArticleDto dto, String newHash) {
        if (StringUtils.hasText(article.getContentHash())) {
            return !Objects.equals(article.getContentHash(), newHash);
        }

        return !Objects.equals(article.getTitle(), dto.title())
                || !Objects.equals(article.getAbstractText(), dto.abstractText())
                || !Objects.equals(article.getJournal(), dto.journal())
                || !Objects.equals(article.getPublishedAt(), dto.publishedAt())
                || !Objects.equals(article.getPublicationDateDisplay(), dto.publishedAtDisplay())
                || !Objects.equals(article.getPublicationType(), dto.publicationType())
                || !Objects.equals(article.getUrl(), dto.url());
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

    private OllamaAnalysisDto buildFallbackAnalysis(ScientificArticleDto dto, String reason) {
        OllamaAnalysisDto fallback = new OllamaAnalysisDto();
        fallback.setSummaryPt(
                "Analise automatica indisponivel no momento. Consulte o titulo, o resumo e o artigo original para avaliacao profissional."
        );
        fallback.setEvidenceType(StringUtils.hasText(dto.publicationType())
                ? dto.publicationType()
                : "Tipo de evidencia nao identificado.");
        fallback.setRelevanceLevel("BAIXO");
        fallback.setPracticalImpact(
                "Impacto pratico nao classificado automaticamente devido a indisponibilidade ou lentidao do modelo local."
        );
        fallback.setWarningNote(
                "Resultado com fallback informacional. Motivo: " + reason
        );
        return fallback;
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

    private String contentHash(ScientificArticleDto dto) {
        String payload = String.join("|",
                safe(dto.source()),
                safe(dto.sourceId()),
                safe(dto.title()),
                safe(dto.abstractText()),
                safe(dto.journal()),
                safe(dto.publishedAt()),
                safe(dto.publishedAtDisplay()),
                safe(dto.publicationType()),
                safe(dto.url())
        );

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponivel para calcular hash do artigo.", ex);
        }
    }

    private String safe(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    private String articleKey(ScientificArticleDto dto) {
        return dto.source() + ":" + dto.sourceId();
    }

    private String resolveSource(String articleKey) {
        int separator = articleKey.indexOf(':');
        if (separator <= 0) {
            return "PUBMED";
        }
        return articleKey.substring(0, separator);
    }

    private String resolveSourceId(String articleKey) {
        int separator = articleKey.indexOf(':');
        if (separator <= 0 || separator == articleKey.length() - 1) {
            return articleKey;
        }
        return articleKey.substring(separator + 1);
    }
}
