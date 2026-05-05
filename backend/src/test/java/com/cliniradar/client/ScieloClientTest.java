package com.cliniradar.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.cliniradar.config.ScieloProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ScieloClientTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private ScieloClient scieloClient;

    @BeforeEach
    void setUp() {
        ScieloProperties properties = new ScieloProperties();
        properties.setBaseUrl("https://articlemeta.scielo.org/api/v1");
        properties.setCollections(List.of("scl"));
        properties.setHarvestSize(1);
        properties.setMaxResults(1);

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(URI.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.accept(any(MediaType.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        scieloClient = new ScieloClient(restClient, properties, new ObjectMapper());
    }

    @Test
    void preservesYearOnlyPublicationDateForDisplay() {
        String identifiersResponse = """
                {
                  "meta": { "total": 1 },
                  "objects": [
                    { "code": "S1678-69712026000200206", "collection": "scl" }
                  ]
                }
                """;

        String articleResponse = """
                {
                  "code": "S1678-69712026000200206",
                  "collection": "scl",
                  "publication_date": "2026",
                  "document_type": "research-article",
                  "doi": "10.1590/1678-6971/ERAMR2620079",
                  "article": {
                    "v12": [{ "l": "en", "_": "Sample SciELO article" }],
                    "v30": [{ "_": "Revista de Teste" }],
                    "v83": [{ "l": "en", "a": "Abstract text", "_": "" }]
                  },
                  "fulltexts": {
                    "html": { "en": "https://www.scielo.br/j/teste/a/abc123/" }
                  }
                }
                """;

        when(responseSpec.body(String.class)).thenReturn(
                identifiersResponse,
                identifiersResponse,
                articleResponse
        );

        var articles = scieloClient.searchArticles("sample article");

        assertThat(articles).hasSize(1);
        assertThat(articles.getFirst().source()).isEqualTo("SCIELO");
        assertThat(articles.getFirst().publishedAt()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(articles.getFirst().publishedAtDisplay()).isEqualTo("2026");
    }
}
