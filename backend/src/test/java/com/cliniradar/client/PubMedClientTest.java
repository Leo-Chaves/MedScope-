package com.cliniradar.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cliniradar.config.PubMedProperties;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PubMedClientTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private PubMedClient pubMedClient;

    @BeforeEach
    void setUp() {
        PubMedProperties properties = new PubMedProperties();
        properties.setBaseUrl("https://eutils.ncbi.nlm.nih.gov/entrez/eutils");
        properties.setMaxResults(2);

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(URI.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.accept(any(MediaType.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        pubMedClient = new PubMedClient(restClient, properties);
    }

    @Test
    void fallsBackToPubMedPmidPageWhenESearchReturnsError() {
        String esearchErrorXml = """
                <?xml version="1.0" encoding="UTF-8" ?>
                <eSearchResult>
                    <ERROR>Search Backend failed: HTTP Request processing error: null</ERROR>
                </eSearchResult>
                """;

        String pubMedPmidHtml = """
                <html>
                  <body>
                    <pre class="search-results-chunk">42055497
                42018894
                41931335</pre>
                  </body>
                </html>
                """;

        String efetchXml = """
                <?xml version="1.0" ?>
                <PubmedArticleSet>
                  <PubmedArticle>
                    <MedlineCitation>
                      <PMID>42055497</PMID>
                      <Article>
                        <Journal>
                          <JournalIssue>
                            <PubDate><Year>2026</Year><Month>Apr</Month><Day>20</Day></PubDate>
                          </JournalIssue>
                          <Title>Clinical Gastroenterology</Title>
                        </Journal>
                        <ArticleTitle>Blood markers in ulcerative colitis.</ArticleTitle>
                        <Abstract><AbstractText>Study abstract.</AbstractText></Abstract>
                        <PublicationTypeList>
                          <PublicationType>Journal Article</PublicationType>
                        </PublicationTypeList>
                      </Article>
                    </MedlineCitation>
                  </PubmedArticle>
                  <PubmedArticle>
                    <MedlineCitation>
                      <PMID>42018894</PMID>
                      <Article>
                        <Journal>
                          <JournalIssue>
                            <PubDate><Year>2026</Year><Month>Apr</Month><Day>10</Day></PubDate>
                          </JournalIssue>
                          <Title>Inflammatory Bowel Diseases</Title>
                        </Journal>
                        <ArticleTitle>Management of ulcerative colitis.</ArticleTitle>
                        <Abstract><AbstractText>Another abstract.</AbstractText></Abstract>
                        <PublicationTypeList>
                          <PublicationType>Review</PublicationType>
                        </PublicationTypeList>
                      </Article>
                    </MedlineCitation>
                  </PubmedArticle>
                </PubmedArticleSet>
                """;

        when(responseSpec.body(String.class)).thenReturn(esearchErrorXml, pubMedPmidHtml, efetchXml);

        var articles = pubMedClient.searchArticles("ulcerative colitis management inflammatory bowel disease blood");

        assertThat(articles).hasSize(2);
        assertThat(articles).extracting("pubmedId").containsExactly("42055497", "42018894");

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(requestHeadersUriSpec, times(3)).uri(uriCaptor.capture());
        List<String> requestedUris = uriCaptor.getAllValues().stream()
                .map(URI::toString)
                .toList();

        assertThat(requestedUris.get(0)).contains("/esearch.fcgi");
        assertThat(requestedUris.get(1)).contains("pubmed.ncbi.nlm.nih.gov");
        assertThat(requestedUris.get(1)).contains("format=pmid");
        assertThat(requestedUris.get(2)).contains("/efetch.fcgi");
        assertThat(requestedUris.get(2)).contains("42055497,42018894");
    }
}
