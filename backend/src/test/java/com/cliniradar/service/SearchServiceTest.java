package com.cliniradar.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.cliniradar.dto.ArticleResponseDto;
import com.cliniradar.dto.SearchRequestDto;
import com.cliniradar.dto.ScientificArticleDto;
import com.cliniradar.entity.Article;
import com.cliniradar.entity.ArticleSummary;
import com.cliniradar.entity.CidMapping;
import com.cliniradar.repository.SearchRequestRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private CidMappingService cidMappingService;

    @Mock
    private SearchRequestRepository searchRequestRepository;

    @Mock
    private ScientificArticleSearchService scientificArticleSearchService;

    @Mock
    private ArticleProcessingService articleProcessingService;

    @Mock
    private CidArticleCacheService cidArticleCacheService;

    private SearchService searchService;

    @BeforeEach
    void setUp() {
        searchService = new SearchService(
                cidMappingService,
                searchRequestRepository,
                articleProcessingService,
                cidArticleCacheService,
                scientificArticleSearchService
        );
    }

    @Test
    void returnsArticleEvenWhenAnalysisUsesInformationalFallback() {
        SearchRequestDto request = new SearchRequestDto();
        request.setCid("K51.9");
        request.setContext("blood");

        CidMapping mapping = new CidMapping(
                "K51.9",
                "Ulcerative colitis",
                "ulcerative colitis management inflammatory bowel disease",
                "Gastroenterology"
        );

        ScientificArticleDto pubMedArticle = new ScientificArticleDto(
                "PUBMED",
                "12345678",
                "Ulcerative colitis treatment review",
                "Abstract text",
                "Clinical Gastroenterology",
                LocalDate.of(2026, 1, 15),
                "2026-01-15",
                "Review",
                "https://pubmed.ncbi.nlm.nih.gov/12345678/"
        );

        Article article = new Article(
                pubMedArticle.source() + ":" + pubMedArticle.sourceId(),
                pubMedArticle.title(),
                pubMedArticle.abstractText(),
                pubMedArticle.journal(),
                pubMedArticle.publishedAt(),
                pubMedArticle.publishedAtDisplay(),
                pubMedArticle.publicationType(),
                pubMedArticle.url()
        );

        ArticleSummary fallbackSummary = new ArticleSummary(article);
        fallbackSummary.setSummaryPt("Analise automatica indisponivel no momento.");
        fallbackSummary.setEvidenceType("Review");
        fallbackSummary.setRelevanceLevel("BAIXO");
        fallbackSummary.setPracticalImpact("Impacto pratico nao classificado automaticamente.");
        fallbackSummary.setWarningNote("Resultado com fallback informacional. Motivo: Falha de comunicacao com o Ollama.");

        ArticleResponseDto responseArticle = new ArticleResponseDto(
                pubMedArticle.source(),
                pubMedArticle.sourceId(),
                pubMedArticle.title(),
                pubMedArticle.publishedAtDisplay(),
                pubMedArticle.publicationType(),
                pubMedArticle.journal(),
                pubMedArticle.url(),
                fallbackSummary.getSummaryPt(),
                fallbackSummary.getRelevanceLevel(),
                fallbackSummary.getEvidenceType(),
                fallbackSummary.getPracticalImpact(),
                fallbackSummary.getWarningNote()
        );

        when(cidMappingService.normalize("K51.9")).thenReturn("K51.9");
        when(cidMappingService.getByCode("K51.9")).thenReturn(mapping);
        when(searchRequestRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(scientificArticleSearchService.searchAcrossSources(
                "ulcerative colitis management inflammatory bowel disease blood",
                SearchRequestDto.SOURCE_BOTH
        ))
                .thenReturn(List.of(pubMedArticle));
        when(articleProcessingService.saveAndAnalyzeArticle(pubMedArticle)).thenReturn(fallbackSummary);
        when(articleProcessingService.toResponse(fallbackSummary)).thenReturn(responseArticle);

        var response = searchService.search(request);

        assertThat(response.articles()).hasSize(1);
        assertThat(response.articles().getFirst().source()).isEqualTo("PUBMED");
        assertThat(response.articles().getFirst().sourceId()).isEqualTo("12345678");
        assertThat(response.articles().getFirst().warningNote()).contains("fallback informacional");
    }
}
