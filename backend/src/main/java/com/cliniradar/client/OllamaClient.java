package com.cliniradar.client;

import com.cliniradar.config.AppProperties;
import com.cliniradar.dto.OllamaAnalysisDto;
import com.cliniradar.exception.ExternalServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.SocketTimeoutException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.ResourceAccessException;

@Component
public class OllamaClient {

    private static final Logger log = LoggerFactory.getLogger(OllamaClient.class);
    private static final int MIN_RETRY_TOKENS = 320;

    private final RestClient restClient;
    private final AppProperties properties;
    private final ObjectMapper objectMapper;

    public OllamaClient(@Qualifier("ollamaRestClient") RestClient restClient,
                        AppProperties properties,
                        ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public OllamaAnalysisDto analyzeArticle(String prompt) {
        try {
            return analyzeArticle(prompt, properties.getMaxTokens(), true);
        } catch (ExternalServiceException ex) {
            throw ex;
        } catch (ResourceAccessException ex) {
            throw translateTransportException(ex);
        } catch (RestClientException ex) {
            throw translateTransportException(ex);
        } catch (Exception ex) {
            if (hasTimeoutCause(ex)) {
                throw new ExternalServiceException(
                        "O Ollama excedeu o tempo limite de resposta. Tente novamente ou reduza o volume analisado.",
                        ex
                );
            }
            log.error("Falha ao processar resposta do Ollama.", ex);
            throw new ExternalServiceException("Falha ao processar a análise do Ollama.", ex);
        }
    }

    private OllamaAnalysisDto analyzeArticle(String prompt, int maxTokens, boolean allowRetry) throws Exception {
        JsonNode envelope = requestAnalysis(prompt, maxTokens);
        String rawResponse = envelope.path("response").asText(null);
        if (rawResponse == null || rawResponse.isBlank()) {
            throw new ExternalServiceException("O Ollama não retornou o campo response com conteúdo.");
        }

        boolean truncated = isTruncated(envelope);
        try {
            String sanitizedJson = extractJsonObject(rawResponse);
            OllamaAnalysisDto dto = objectMapper.readValue(sanitizedJson, OllamaAnalysisDto.class);
            normalizeMissingFields(dto);
            return dto;
        } catch (ExternalServiceException ex) {
            if (allowRetry && truncated) {
                int retryTokens = Math.max(maxTokens * 2, MIN_RETRY_TOKENS);
                log.warn("Resposta do Ollama truncada com {} tokens. Nova tentativa com {} tokens.", maxTokens, retryTokens);
                return analyzeArticle(prompt, retryTokens, false);
            }
            throw ex;
        }
    }

    private JsonNode requestAnalysis(String prompt, int maxTokens) throws Exception {
        String responseBody = restClient.post()
                .uri(properties.getBaseUrl() + "/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "model", properties.getModel(),
                        "prompt", prompt,
                        "stream", false,
                        "format", "json",
                        "keep_alive", properties.getKeepAlive(),
                        "options", Map.of(
                                "num_predict", maxTokens,
                                "temperature", 0.1
                        )
                ))
                .retrieve()
                .body(String.class);

        if (responseBody == null || responseBody.isBlank()) {
            throw new ExternalServiceException("O Ollama retornou uma resposta vazia.");
        }

        String envelopeJson = extractJsonObject(responseBody.trim());
        return objectMapper.readTree(envelopeJson);
    }

    private boolean isTruncated(JsonNode envelope) {
        return "length".equalsIgnoreCase(envelope.path("done_reason").asText(""));
    }

    private ExternalServiceException translateTransportException(Exception ex) {
        if (hasTimeoutCause(ex)) {
            return new ExternalServiceException(
                    "O Ollama excedeu o tempo limite de resposta. Tente novamente ou reduza o volume analisado.",
                    ex
            );
        }
        return new ExternalServiceException("Falha de comunicação com o Ollama.", ex);
    }

    private boolean hasTimeoutCause(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SocketTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String extractJsonObject(String rawResponse) {
        String normalized = rawResponse
                .replace("```json", "")
                .replace("```", "")
                .trim();

        int start = normalized.indexOf('{');
        int end = normalized.lastIndexOf('}');

        if (start >= 0 && end > start) {
            return normalized.substring(start, end + 1);
        }

        throw new ExternalServiceException("O Ollama não retornou um JSON válido.");
    }

    private void normalizeMissingFields(OllamaAnalysisDto dto) {
        if (dto.getSummaryPt() == null || dto.getSummaryPt().isBlank()) {
            dto.setSummaryPt("Resumo informativo indisponível. Consulte o artigo original.");
        }
        if (dto.getEvidenceType() == null || dto.getEvidenceType().isBlank()) {
            dto.setEvidenceType("Tipo de evidência não identificado.");
        }
        if (dto.getRelevanceLevel() == null || dto.getRelevanceLevel().isBlank()) {
            dto.setRelevanceLevel("BAIXO");
        }
        if (dto.getPracticalImpact() == null || dto.getPracticalImpact().isBlank()) {
            dto.setPracticalImpact("Impacto prático incerto; interpretar com cautela.");
        }
        if (dto.getWarningNote() == null || dto.getWarningNote().isBlank()) {
            dto.setWarningNote("Material apenas informativo para avaliação profissional.");
        }
    }
}
