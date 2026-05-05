package com.cliniradar.service;

import com.cliniradar.dto.ScientificArticleDto;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PromptBuilderService {

    private static final int MAX_ABSTRACT_CHARS = 1600;

    public String buildArticleAnalysisPrompt(ScientificArticleDto article) {
        String abstractText = StringUtils.hasText(article.abstractText())
                ? article.abstractText()
                : "Resumo nao disponivel.";
        String compactAbstract = abbreviate(abstractText, MAX_ABSTRACT_CHARS);

        return """
                Analise o artigo abaixo e retorne somente um JSON valido em portugues do Brasil.

                Formato:
                {
                  "summaryPt": "string",
                  "evidenceType": "string",
                  "relevanceLevel": "ALTO|MEDIO|BAIXO",
                  "practicalImpact": "string",
                  "warningNote": "string"
                }

                Regras:
                - Nao recomende tratamento.
                - Nao invente dados.
                - Se o estudo for inconclusivo, diga isso.
                - Seja breve e objetivo.
                - summaryPt deve ter, no maximo, 5 linhas curtas e linguagem simples.
                - Em relevanceLevel, use somente ALTO, MEDIO ou BAIXO.
                - Em evidenceType, traduza o tipo de evidencia para portugues do Brasil; por exemplo, "Randomized Controlled Trial" vira "Ensaio Clinico Randomizado".
                - Se nao houver dados suficientes, explicite a limitacao.
                - Nao escreva nada fora do JSON.

                Artigo:
                Titulo: %s
                Tipo de publicacao: %s
                Periodico: %s
                Data: %s
                Resumo: %s
                """.formatted(
                article.title(),
                article.publicationType(),
                article.journal(),
                article.publishedAt(),
                compactAbstract
        );
    }

    private String abbreviate(String value, int maxChars) {
        if (!StringUtils.hasText(value) || value.length() <= maxChars) {
            return value;
        }
        return value.substring(0, maxChars) + "... [resumo truncado para analise rapida]";
    }
}
