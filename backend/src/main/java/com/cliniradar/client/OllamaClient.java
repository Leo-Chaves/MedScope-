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

    public String translateMedicalTerm(String term) {
        String prompt = """
                Traduza para ingles medico tecnico, retornando apenas o termo traduzido sem explicacoes: "%s"
                """.formatted(term);
        try {
            String translated = requestText(prompt, 80)
                    .replace("\"", "")
                    .trim();
            if (translated.isBlank()) {
                throw new ExternalServiceException("O Ollama retornou uma traducao vazia.");
            }
            return translated.lines().findFirst().orElse(translated).trim();
        } catch (ExternalServiceException ex) {
            throw ex;
        } catch (ResourceAccessException ex) {
            throw translateTransportException(ex);
        } catch (RestClientException ex) {
            throw translateTransportException(ex);
        } catch (Exception ex) {
            log.error("Falha ao traduzir termo livre com Ollama.", ex);
            throw new ExternalServiceException("Falha ao traduzir termo livre com o Ollama.", ex);
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
                try {
                    return analyzeArticle(prompt, retryTokens, false);
                } catch (ExternalServiceException retryEx) {
                    int strictRetryTokens = Math.max(retryTokens * 2, MIN_RETRY_TOKENS);
                    log.warn("Retry apos truncamento ainda retornou JSON invalido. Nova tentativa com prompt estrito e {} tokens.", strictRetryTokens);
                    return analyzeArticle(strictJsonPrompt(prompt), strictRetryTokens, false);
                }
            }
            if (allowRetry) {
                int retryTokens = Math.max(maxTokens * 2, MIN_RETRY_TOKENS);
                log.warn("Ollama retornou JSON invalido. Nova tentativa com prompt estrito e {} tokens.", retryTokens);
                return analyzeArticle(strictJsonPrompt(prompt), retryTokens, false);
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

    private String requestText(String prompt, int maxTokens) throws Exception {
        String responseBody = restClient.post()
                .uri(properties.getBaseUrl() + "/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "model", properties.getModel(),
                        "prompt", prompt,
                        "stream", false,
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

        JsonNode envelope = objectMapper.readTree(extractJsonObject(responseBody.trim()));
        String response = envelope.path("response").asText(null);
        if (response == null || response.isBlank()) {
            throw new ExternalServiceException("O Ollama nao retornou o campo response com conteudo.");
        }
        return response;
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

    private String strictJsonPrompt(String originalPrompt) {
        return """
                Retorne exclusivamente um objeto JSON valido.
                Nao use markdown.
                Nao escreva explicacoes.
                Nao use texto antes ou depois do JSON.
                Nao use quebras que invalidem strings.
                Use exatamente estas chaves:
                summaryPt, evidenceType, relevanceLevel, practicalImpact, warningNote.

                Exemplo de formato:
                {
                  "summaryPt": "Resumo curto em portugues.",
                  "evidenceType": "Tipo de evidencia.",
                  "relevanceLevel": "BAIXO",
                  "practicalImpact": "Impacto pratico cauteloso.",
                  "warningNote": "Nota de cautela."
                }

                Tarefa original:
                %s
                """.formatted(originalPrompt);
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
