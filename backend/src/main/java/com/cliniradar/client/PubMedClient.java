package com.cliniradar.client;

import com.cliniradar.config.PubMedProperties;
import com.cliniradar.dto.ScientificArticleDto;
import com.cliniradar.exception.ExternalServiceException;
import java.io.StringReader;
import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

@Component
public class PubMedClient implements ScientificArticleSearchClient {

    private static final Logger log = LoggerFactory.getLogger(PubMedClient.class);
    private static final String PUBMED_SEARCH_URL = "https://pubmed.ncbi.nlm.nih.gov/";
    private static final Pattern PMID_CHUNK_PATTERN = Pattern.compile(
            "<pre\\s+class=\"search-results-chunk\">([\\s\\S]*?)</pre>",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern PMID_PATTERN = Pattern.compile("\\b\\d{6,}\\b");

    private final RestClient restClient;
    private final PubMedProperties properties;

    public PubMedClient(RestClient restClient, PubMedProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public List<ScientificArticleDto> searchArticles(String query) {
        List<String> ids = fetchIds(query);
        if (ids.isEmpty()) {
            return List.of();
        }
        return fetchArticles(ids);
    }

    private List<String> fetchIds(String query) {
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
                    .path("/esearch.fcgi")
                    .queryParam("db", "pubmed")
                    .queryParam("sort", "pub date")
                    .queryParam("retmode", "xml")
                    .queryParam("retmax", properties.getMaxResults())
                    .queryParam("term", query)
                    .build()
                    .encode()
                    .toUri();

            String xml = restClient.get()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_XML)
                    .retrieve()
                    .body(String.class);

            Document document = parseXml(xml);
            String error = firstText(document.getDocumentElement(), "ERROR");
            if (StringUtils.hasText(error)) {
                log.warn("ESearch do PubMed retornou erro: {}. Tentando fallback por pagina PMID.", error.trim());
                return fetchIdsFromPubMedSearchPage(query);
            }

            NodeList idNodes = document.getElementsByTagName("Id");
            List<String> ids = new ArrayList<>();
            for (int index = 0; index < idNodes.getLength(); index++) {
                ids.add(idNodes.item(index).getTextContent().trim());
            }
            return ids;
        } catch (Exception ex) {
            log.warn("Falha ao consultar ESearch do PubMed. Tentando fallback por pagina PMID.", ex);
            return fetchIdsFromPubMedSearchPage(query);
        }
    }

    private List<String> fetchIdsFromPubMedSearchPage(String query) {
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(PUBMED_SEARCH_URL)
                    .queryParam("term", query)
                    .queryParam("sort", "date")
                    .queryParam("format", "pmid")
                    .build()
                    .encode()
                    .toUri();

            String html = restClient.get()
                    .uri(uri)
                    .accept(MediaType.TEXT_HTML)
                    .retrieve()
                    .body(String.class);

            if (!StringUtils.hasText(html)) {
                return List.of();
            }

            Matcher chunkMatcher = PMID_CHUNK_PATTERN.matcher(html);
            String pmidChunk = chunkMatcher.find() ? chunkMatcher.group(1) : html;
            Matcher pmidMatcher = PMID_PATTERN.matcher(pmidChunk);
            List<String> ids = new ArrayList<>();
            while (pmidMatcher.find() && ids.size() < properties.getMaxResults()) {
                ids.add(pmidMatcher.group());
            }
            return ids;
        } catch (Exception ex) {
            throw new ExternalServiceException("Falha ao consultar o PubMed.", ex);
        }
    }

    private List<ScientificArticleDto> fetchArticles(List<String> ids) {
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
                    .path("/efetch.fcgi")
                    .queryParam("db", "pubmed")
                    .queryParam("retmode", "xml")
                    .queryParam("id", String.join(",", ids))
                    .build()
                    .encode()
                    .toUri();

            String xml = restClient.get()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_XML)
                    .retrieve()
                    .body(String.class);

            Document document = parseXml(xml);
            NodeList articleNodes = document.getElementsByTagName("PubmedArticle");
            List<ScientificArticleDto> articles = new ArrayList<>();

            for (int index = 0; index < articleNodes.getLength(); index++) {
                try {
                    Element article = (Element) articleNodes.item(index);
                    String pubmedId = firstText(article, "PMID");
                    String title = firstText(article, "ArticleTitle");
                    String abstractText = collectAbstract(article);
                    String journal = firstText(article, "Title");
                    String publicationType = collectPublicationTypes(article);
                    LocalDate publishedAt = parseDate(article);

                    articles.add(new ScientificArticleDto(
                            "PUBMED",
                            pubmedId,
                            StringUtils.hasText(title) ? title : "Sem titulo informado.",
                            StringUtils.hasText(abstractText)
                                    ? abstractText
                                    : "Resumo nao disponivel no PubMed para este artigo.",
                            StringUtils.hasText(journal) ? journal : "Periodico nao informado.",
                            publishedAt,
                            publishedAt != null ? publishedAt.toString() : null,
                            StringUtils.hasText(publicationType) ? publicationType : "Nao informado.",
                            "https://pubmed.ncbi.nlm.nih.gov/" + pubmedId + "/"
                    ));
                } catch (Exception articleEx) {
                    log.warn("Falha ao converter artigo do PubMed. O item sera ignorado.", articleEx);
                }
            }

            return articles;
        } catch (Exception ex) {
            log.error("Falha ao obter detalhes dos artigos no PubMed.", ex);
            throw new ExternalServiceException("Falha ao obter detalhes dos artigos no PubMed.", ex);
        }
    }

    private Document parseXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        var builder = factory.newDocumentBuilder();
        builder.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));
        return builder.parse(new InputSource(new StringReader(xml)));
    }

    private String collectAbstract(Element article) {
        NodeList abstractTextNodes = article.getElementsByTagName("AbstractText");
        if (abstractTextNodes.getLength() == 0) {
            return null;
        }

        List<String> parts = new ArrayList<>();
        for (int i = 0; i < abstractTextNodes.getLength(); i++) {
            Node node = abstractTextNodes.item(i);
            String label = node.getAttributes() != null && node.getAttributes().getNamedItem("Label") != null
                    ? node.getAttributes().getNamedItem("Label").getTextContent()
                    : null;
            String text = node.getTextContent();
            if (StringUtils.hasText(label)) {
                parts.add(label + ": " + text);
            } else if (StringUtils.hasText(text)) {
                parts.add(text);
            }
        }
        return String.join(" ", parts);
    }

    private String collectPublicationTypes(Element article) {
        NodeList typeNodes = article.getElementsByTagName("PublicationType");
        List<String> types = new ArrayList<>();
        for (int i = 0; i < typeNodes.getLength(); i++) {
            String value = typeNodes.item(i).getTextContent();
            if (StringUtils.hasText(value)) {
                types.add(value.trim());
            }
        }
        return String.join(", ", types);
    }

    private LocalDate parseDate(Element article) {
        LocalDate pubDate = parsePubDate(article);
        if (pubDate == null || !pubDate.isAfter(LocalDate.now())) {
            return pubDate;
        }

        LocalDate articleDate = parseFirstDate(article, "ArticleDate");
        if (articleDate != null && !articleDate.isAfter(LocalDate.now())) {
            return articleDate;
        }

        LocalDate historyDate = parsePubMedHistoryDate(article);
        if (historyDate != null && !historyDate.isAfter(LocalDate.now())) {
            return historyDate;
        }

        return null;
    }

    private LocalDate parsePubDate(Element article) {
        NodeList pubDateNodes = article.getElementsByTagName("PubDate");
        if (pubDateNodes.getLength() == 0) {
            return null;
        }

        Element pubDate = (Element) pubDateNodes.item(0);
        String year = firstText(pubDate, "Year");
        String month = firstText(pubDate, "Month");
        String day = firstText(pubDate, "Day");

        if (!StringUtils.hasText(year)) {
            String medlineDate = firstText(pubDate, "MedlineDate");
            if (StringUtils.hasText(medlineDate) && medlineDate.length() >= 4) {
                return LocalDate.of(Integer.parseInt(medlineDate.substring(0, 4)), 1, 1);
            }
            return null;
        }

        try {
            int parsedYear = Integer.parseInt(year);
            int parsedMonth = parseMonth(month);
            int parsedDay = StringUtils.hasText(day) ? Integer.parseInt(day) : 1;
            return LocalDate.of(parsedYear, parsedMonth, parsedDay);
        } catch (RuntimeException ex) {
            return LocalDate.of(Integer.parseInt(year), 1, 1);
        }
    }

    private LocalDate parseFirstDate(Element parent, String tagName) {
        NodeList dateNodes = parent.getElementsByTagName(tagName);
        if (dateNodes.getLength() == 0) {
            return null;
        }

        for (int index = 0; index < dateNodes.getLength(); index++) {
            LocalDate parsedDate = parseDateElement((Element) dateNodes.item(index));
            if (parsedDate != null) {
                return parsedDate;
            }
        }
        return null;
    }

    private LocalDate parsePubMedHistoryDate(Element article) {
        NodeList dateNodes = article.getElementsByTagName("PubMedPubDate");
        List<String> preferredStatuses = List.of("pubmed", "entrez", "medline");
        for (String preferredStatus : preferredStatuses) {
            for (int index = 0; index < dateNodes.getLength(); index++) {
                Element dateElement = (Element) dateNodes.item(index);
                String status = dateElement.getAttribute("PubStatus");
                if (preferredStatus.equalsIgnoreCase(status)) {
                    LocalDate parsedDate = parseDateElement(dateElement);
                    if (parsedDate != null) {
                        return parsedDate;
                    }
                }
            }
        }
        return null;
    }

    private LocalDate parseDateElement(Element dateElement) {
        String year = firstText(dateElement, "Year");
        if (!StringUtils.hasText(year)) {
            return null;
        }

        try {
            int parsedYear = Integer.parseInt(year);
            int parsedMonth = parseMonth(firstText(dateElement, "Month"));
            String day = firstText(dateElement, "Day");
            int parsedDay = StringUtils.hasText(day) ? Integer.parseInt(day) : 1;
            return LocalDate.of(parsedYear, parsedMonth, parsedDay);
        } catch (RuntimeException ex) {
            return LocalDate.of(Integer.parseInt(year), 1, 1);
        }
    }

    private int parseMonth(String month) {
        if (!StringUtils.hasText(month)) {
            return 1;
        }
        return switch (month.trim().toLowerCase()) {
            case "jan", "january" -> 1;
            case "feb", "february" -> 2;
            case "mar", "march" -> 3;
            case "apr", "april" -> 4;
            case "may" -> 5;
            case "jun", "june" -> 6;
            case "jul", "july" -> 7;
            case "aug", "august" -> 8;
            case "sep", "sept", "september" -> 9;
            case "oct", "october" -> 10;
            case "nov", "november" -> 11;
            case "dec", "december" -> 12;
            default -> Integer.parseInt(month);
        };
    }

    private String firstText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) {
            return null;
        }
        return nodes.item(0).getTextContent();
    }
}
