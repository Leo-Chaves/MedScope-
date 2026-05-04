package com.cliniradar.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cliniradar.config.AppProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
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
class OllamaClientTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private OllamaClient ollamaClient;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties();
        properties.setBaseUrl("http://localhost:11434");
        properties.setModel("mistral");
        properties.setMaxTokens(180);
        properties.setKeepAlive("15m");

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Map.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);

        ollamaClient = new OllamaClient(restClient, properties, new ObjectMapper());
    }

    @Test
    void retriesWhenOllamaStopsByLengthAndReturnsTruncatedJson() {
        String truncatedEnvelope = """
                {
                  "response": "{\\"summaryPt\\":\\"Resumo truncado",
                  "done_reason": "length"
                }
                """;

        String completedEnvelope = """
                {
                  "response": "{\\"summaryPt\\":\\"Resumo final\\",\\"evidenceType\\":\\"Journal Article\\",\\"relevanceLevel\\":\\"MEDIO\\",\\"practicalImpact\\":\\"Impacto pratico moderado.\\",\\"warningNote\\":\\"Use como apoio clinico.\\"}",
                  "done_reason": "stop"
                }
                """;

        when(responseSpec.body(String.class)).thenReturn(truncatedEnvelope, completedEnvelope);

        var result = ollamaClient.analyzeArticle("prompt de teste");

        assertThat(result.getSummaryPt()).isEqualTo("Resumo final");
        assertThat(result.getEvidenceType()).isEqualTo("Journal Article");
        assertThat(result.getRelevanceLevel()).isEqualTo("MEDIO");

        ArgumentCaptor<Map> bodyCaptor = ArgumentCaptor.forClass(Map.class);
        verify(requestBodySpec, times(2)).body(bodyCaptor.capture());

        List<Map> requestBodies = bodyCaptor.getAllValues();
        assertThat(requestBodies).hasSize(2);
        assertThat(((Map<?, ?>) requestBodies.get(0).get("options")).get("num_predict")).isEqualTo(180);
        assertThat(((Map<?, ?>) requestBodies.get(1).get("options")).get("num_predict")).isEqualTo(360);
    }

    @Test
    void retriesWithStrictPromptWhenOllamaReturnsNonJsonText() {
        String invalidEnvelope = """
                {
                  "response": "Claro, aqui esta a analise solicitada.",
                  "done_reason": "stop"
                }
                """;

        String completedEnvelope = """
                {
                  "response": "{\\"summaryPt\\":\\"Resumo final\\",\\"evidenceType\\":\\"Journal Article\\",\\"relevanceLevel\\":\\"BAIXO\\",\\"practicalImpact\\":\\"Impacto pratico incerto.\\",\\"warningNote\\":\\"Use como apoio clinico.\\"}",
                  "done_reason": "stop"
                }
                """;

        when(responseSpec.body(String.class)).thenReturn(invalidEnvelope, completedEnvelope);

        var result = ollamaClient.analyzeArticle("prompt de teste");

        assertThat(result.getSummaryPt()).isEqualTo("Resumo final");

        ArgumentCaptor<Map> bodyCaptor = ArgumentCaptor.forClass(Map.class);
        verify(requestBodySpec, times(2)).body(bodyCaptor.capture());

        List<Map> requestBodies = bodyCaptor.getAllValues();
        assertThat(requestBodies.get(1).get("prompt").toString())
                .contains("Retorne exclusivamente um objeto JSON valido");
    }

    @Test
    void retriesWithStrictPromptWhenRetryAfterTruncationIsStillInvalid() {
        String truncatedEnvelope = """
                {
                  "response": "{\\"summaryPt\\":\\"Resumo truncado",
                  "done_reason": "length"
                }
                """;

        String invalidEnvelope = """
                {
                  "response": "Analise em texto sem objeto JSON.",
                  "done_reason": "stop"
                }
                """;

        String completedEnvelope = """
                {
                  "response": "{\\"summaryPt\\":\\"Resumo final\\",\\"evidenceType\\":\\"Journal Article\\",\\"relevanceLevel\\":\\"BAIXO\\",\\"practicalImpact\\":\\"Impacto pratico incerto.\\",\\"warningNote\\":\\"Use como apoio clinico.\\"}",
                  "done_reason": "stop"
                }
                """;

        when(responseSpec.body(String.class)).thenReturn(truncatedEnvelope, invalidEnvelope, completedEnvelope);

        var result = ollamaClient.analyzeArticle("prompt de teste");

        assertThat(result.getSummaryPt()).isEqualTo("Resumo final");

        ArgumentCaptor<Map> bodyCaptor = ArgumentCaptor.forClass(Map.class);
        verify(requestBodySpec, times(3)).body(bodyCaptor.capture());

        List<Map> requestBodies = bodyCaptor.getAllValues();
        assertThat(requestBodies.get(2).get("prompt").toString())
                .contains("Retorne exclusivamente um objeto JSON valido");
    }
}
