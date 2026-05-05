package com.cliniradar.client;

import com.cliniradar.config.ScieloProperties;
import com.cliniradar.dto.ScientificArticleDto;
import com.cliniradar.exception.ExternalServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class ScieloClient implements ScientificArticleSearchClient {

    private static final Logger log = LoggerFactory.getLogger(ScieloClient.class);
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^\\p{IsAlphabetic}\\p{IsDigit}]+");
    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "and", "as", "at", "by", "for", "from", "in", "of", "on", "or", "the", "to", "via", "with",
            "adult", "adults", "disease", "disorder", "patient", "patients", "study", "treatment",
            "com", "como", "da", "das", "de", "do", "dos", "e", "em", "na", "no", "para", "por"
    );

    private final RestClient restClient;
    private final ScieloProperties properties;
    private final ObjectMapper objectMapper;

    public ScieloClient(RestClient restClient, ScieloProperties properties, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<ScientificArticleDto> searchArticles(String query) {
        try {
            List<String> queryTerms = buildQueryTerms(query);
            if (queryTerms.isEmpty()) {
                return List.of();
            }

            List<ScientificArticleDto> candidates = new ArrayList<>();
            for (String collection : properties.getCollections()) {
                candidates.addAll(fetchRecentCandidates(collection));
            }

            return candidates.stream()
                    .map(article -> new RankedArticle(article, score(article, query, queryTerms)))
                    .filter(ranked -> ranked.score() > 0)
                    .sorted(Comparator
                            .comparingInt(RankedArticle::score).reversed()
                            .thenComparing(ranked -> ranked.article().publishedAt(), Comparator.nullsLast(Comparator.reverseOrder())))
                    .map(RankedArticle::article)
                    .distinct()
                    .limit(properties.getMaxResults())
                    .toList();
        } catch (Exception ex) {
            log.warn("Falha ao consultar a SciELO.", ex);
            return List.of();
        }
    }

    private List<ScientificArticleDto> fetchRecentCandidates(String collection) throws Exception {
        int total = fetchTotalIdentifiers(collection);
        if (total <= 0) {
            return List.of();
        }

        int harvestSize = Math.min(properties.getHarvestSize(), total);
        int offset = Math.max(total - harvestSize, 0);
        List<IdentifierEntry> identifiers = fetchIdentifiers(collection, offset, harvestSize);
        if (identifiers.isEmpty()) {
            return List.of();
        }

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<ScientificArticleDto>> tasks = identifiers.stream()
                    .map(identifier -> executor.submit(() -> fetchArticle(identifier)))
                    .toList();

            List<ScientificArticleDto> articles = new ArrayList<>();
            for (Future<ScientificArticleDto> task : tasks) {
                ScientificArticleDto article = task.get();
                if (article != null) {
                    articles.add(article);
                }
            }
            return articles;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw ex;
        } catch (ExecutionException ex) {
            throw new ExternalServiceException("Falha ao obter detalhes dos artigos da SciELO.", ex.getCause());
        }
    }

    private int fetchTotalIdentifiers(String collection) throws Exception {
        URI uri = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
                .path("/article/identifiers/")
                .queryParam("collection", collection)
                .queryParam("limit", 1)
                .build()
                .encode()
                .toUri();

        JsonNode root = requestJson(uri);
        return root.path("meta").path("total").asInt(0);
    }

    private List<IdentifierEntry> fetchIdentifiers(String collection, int offset, int limit) throws Exception {
        URI uri = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
                .path("/article/identifiers/")
                .queryParam("collection", collection)
                .queryParam("limit", limit)
                .queryParam("offset", offset)
                .build()
                .encode()
                .toUri();

        JsonNode root = requestJson(uri);
        List<IdentifierEntry> identifiers = new ArrayList<>();
        for (JsonNode item : root.path("objects")) {
            String code = item.path("code").asText(null);
            String articleCollection = item.path("collection").asText(collection);
            if (StringUtils.hasText(code)) {
                identifiers.add(new IdentifierEntry(articleCollection, code));
            }
        }
        return identifiers;
    }

    private ScientificArticleDto fetchArticle(IdentifierEntry identifier) throws Exception {
        URI uri = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
                .path("/article/")
                .queryParam("collection", identifier.collection())
                .queryParam("code", identifier.code())
                .build()
                .encode()
                .toUri();

        JsonNode root = requestJson(uri);
        if (root == null || root.isNull() || root.isMissingNode()) {
            return null;
        }

        JsonNode article = root.path("article");
        String title = pickLocalizedValue(article.path("v12"));
        String abstractText = extractAbstract(article.path("v83"));
        String journal = firstUnderscore(article.path("v30"));
        String publicationDateRaw = root.path("publication_date").asText(null);
        LocalDate publishedAt = parsePublicationDate(publicationDateRaw);
        String publicationType = humanize(root.path("document_type").asText("Artigo científico"));
        String url = extractUrl(root.path("fulltexts"), root.path("doi").asText(null), identifier.code());

        if (!StringUtils.hasText(title) || !StringUtils.hasText(url)) {
            return null;
        }

        return new ScientificArticleDto(
                "SCIELO",
                identifier.code(),
                title,
                abstractText,
                StringUtils.hasText(journal) ? journal : "SciELO",
                publishedAt,
                formatPublicationDateDisplay(publicationDateRaw, publishedAt),
                publicationType,
                url
        );
    }

    private JsonNode requestJson(URI uri) throws Exception {
        String body = restClient.get()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(String.class);

        if (!StringUtils.hasText(body) || "null".equalsIgnoreCase(body.trim())) {
            return null;
        }
        return objectMapper.readTree(body);
    }

    private List<String> buildQueryTerms(String query) {
        Set<String> uniqueTerms = new HashSet<>();
        for (String token : normalize(query).split("\\s+")) {
            if (token.length() < 4 || STOP_WORDS.contains(token)) {
                continue;
            }
            uniqueTerms.add(token);
        }
        return new ArrayList<>(uniqueTerms);
    }

    private int score(ScientificArticleDto article, String fullQuery, List<String> terms) {
        String normalizedQuery = normalize(fullQuery);
        String normalizedTitle = normalize(article.title());
        String normalizedAbstract = normalize(article.abstractText());
        String normalizedJournal = normalize(article.journal());

        int score = 0;
        if (StringUtils.hasText(normalizedQuery) && normalizedTitle.contains(normalizedQuery)) {
            score += 20;
        }

        for (String term : terms) {
            if (normalizedTitle.contains(term)) {
                score += 6;
            }
            if (normalizedAbstract.contains(term)) {
                score += 3;
            }
            if (normalizedJournal.contains(term)) {
                score += 1;
            }
        }
        return score;
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
        return NON_ALPHANUMERIC.matcher(normalized).replaceAll(" ").trim();
    }

    private String pickLocalizedValue(JsonNode valuesNode) {
        if (!valuesNode.isArray()) {
            return null;
        }

        String fallback = null;
        for (JsonNode item : valuesNode) {
            String language = item.path("l").asText("");
            String value = item.path("_").asText(null);
            if (!StringUtils.hasText(value)) {
                continue;
            }
            if ("en".equalsIgnoreCase(language)) {
                return value;
            }
            if (fallback == null) {
                fallback = value;
            }
        }
        return fallback;
    }

    private String extractAbstract(JsonNode abstractNode) {
        if (!abstractNode.isArray()) {
            return null;
        }

        List<String> parts = new ArrayList<>();
        for (JsonNode item : abstractNode) {
            String value = item.path("a").asText(null);
            if (StringUtils.hasText(value)) {
                parts.add(value);
            }
        }
        return parts.isEmpty() ? null : String.join(" ", parts);
    }

    private String firstUnderscore(JsonNode valuesNode) {
        if (!valuesNode.isArray() || valuesNode.isEmpty()) {
            return null;
        }
        String value = valuesNode.get(0).path("_").asText(null);
        return StringUtils.hasText(value) ? value : null;
    }

    private LocalDate parsePublicationDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        try {
            if (value.length() == 7) {
                return LocalDate.parse(value + "-01", DateTimeFormatter.ISO_LOCAL_DATE);
            }
            return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException ex) {
            if (value.length() >= 4) {
                return LocalDate.of(Integer.parseInt(value.substring(0, 4)), 1, 1);
            }
            return null;
        }
    }

    private String formatPublicationDateDisplay(String rawValue, LocalDate parsedDate) {
        if (StringUtils.hasText(rawValue)) {
            String normalized = rawValue.trim();
            if (normalized.matches("\\d{4}") || normalized.matches("\\d{4}-\\d{2}")
                    || normalized.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return normalized;
            }
        }
        return parsedDate != null ? parsedDate.toString() : null;
    }

    private String humanize(String value) {
        if (!StringUtils.hasText(value)) {
            return "Artigo científico";
        }

        String normalized = value.replace('-', ' ').trim();
        if (normalized.isEmpty()) {
            return "Artigo científico";
        }

        String[] parts = normalized.split("\\s+");
        List<String> capitalized = new ArrayList<>(parts.length);
        for (String part : parts) {
            capitalized.add(part.substring(0, 1).toUpperCase(Locale.ROOT) + part.substring(1).toLowerCase(Locale.ROOT));
        }
        return String.join(" ", capitalized);
    }

    private String extractUrl(JsonNode fulltextsNode, String doi, String sourceId) {
        for (String format : List.of("html", "pdf")) {
            JsonNode formatNode = fulltextsNode.path(format);
            if (formatNode.isObject()) {
                for (String language : List.of("en", "pt", "es")) {
                    String url = formatNode.path(language).asText(null);
                    if (StringUtils.hasText(url)) {
                        return url.replace("http://", "https://");
                    }
                }
                var fields = formatNode.fields();
                while (fields.hasNext()) {
                    String url = fields.next().getValue().asText(null);
                    if (StringUtils.hasText(url)) {
                        return url.replace("http://", "https://");
                    }
                }
            }
        }

        if (StringUtils.hasText(doi)) {
            return "https://doi.org/" + doi;
        }

        return StringUtils.hasText(sourceId)
                ? "https://www.scielo.org/en/search/?q=pid:" + sourceId
                : null;
    }

    private record IdentifierEntry(String collection, String code) {
    }

    private record RankedArticle(ScientificArticleDto article, int score) {
    }
}
