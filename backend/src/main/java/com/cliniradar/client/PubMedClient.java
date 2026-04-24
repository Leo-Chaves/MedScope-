package com.cliniradar.client;

import com.cliniradar.config.PubMedProperties;
import com.cliniradar.dto.PubMedArticleDto;
import com.cliniradar.exception.ExternalServiceException;
import java.io.StringReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

@Component
public class PubMedClient {

    private static final Logger log = LoggerFactory.getLogger(PubMedClient.class);

    private final RestClient restClient;
    private final PubMedProperties properties;

    public PubMedClient(RestClient restClient, PubMedProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public List<PubMedArticleDto> searchArticles(String query) {
        List<String> ids = fetchIds(query);
        if (ids.isEmpty()) {
            return List.of();
        }
        return fetchArticles(ids);
    }

    private List<String> fetchIds(String query) {
        try {
            String xml = restClient.get()
                    .uri(properties.getBaseUrl()
                            + "/esearch.fcgi?db=pubmed&sort=pub%20date&retmode=xml&retmax="
                            + properties.getMaxResults()
                            + "&term="
                            + URLEncoder.encode(query, StandardCharsets.UTF_8))
                    .accept(MediaType.APPLICATION_XML)
                    .retrieve()
                    .body(String.class);

            Document document = parseXml(xml);
            NodeList idNodes = document.getElementsByTagName("Id");
            List<String> ids = new ArrayList<>();
            for (int index = 0; index < idNodes.getLength(); index++) {
                ids.add(idNodes.item(index).getTextContent().trim());
            }
            return ids;
        } catch (Exception ex) {
            throw new ExternalServiceException("Falha ao consultar o PubMed.", ex);
        }
    }

    private List<PubMedArticleDto> fetchArticles(List<String> ids) {
        try {
            String xml = restClient.get()
                    .uri(properties.getBaseUrl()
                            + "/efetch.fcgi?db=pubmed&retmode=xml&id="
                            + String.join(",", ids))
                    .accept(MediaType.APPLICATION_XML)
                    .retrieve()
                    .body(String.class);

            Document document = parseXml(xml);
            NodeList articleNodes = document.getElementsByTagName("PubmedArticle");
            List<PubMedArticleDto> articles = new ArrayList<>();

            for (int index = 0; index < articleNodes.getLength(); index++) {
                try {
                    Element article = (Element) articleNodes.item(index);
                    String pubmedId = firstText(article, "PMID");
                    String title = firstText(article, "ArticleTitle");
                    String abstractText = collectAbstract(article);
                    String journal = firstText(article, "Title");
                    String publicationType = collectPublicationTypes(article);
                    LocalDate publishedAt = parseDate(article);

                    articles.add(new PubMedArticleDto(
                            pubmedId,
                            StringUtils.hasText(title) ? title : "Sem título informado.",
                            StringUtils.hasText(abstractText)
                                    ? abstractText
                                    : "Resumo não disponível no PubMed para este artigo.",
                            StringUtils.hasText(journal) ? journal : "Periódico não informado.",
                            publishedAt,
                            StringUtils.hasText(publicationType) ? publicationType : "Não informado.",
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
