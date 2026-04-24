package com.cliniradar.service;

import com.cliniradar.dto.PubMedArticleDto;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PromptBuilderService {

    private static final int MAX_ABSTRACT_CHARS = 1600;

    public String buildArticleAnalysisPrompt(PubMedArticleDto article) {
        String abstractText = StringUtils.hasText(article.abstractText())
                ? article.abstractText()
                : "Resumo não disponível.";
        String compactAbstract = abbreviate(abstractText, MAX_ABSTRACT_CHARS);

        return """
                Analise o artigo abaixo e retorne somente um JSON válido em português do Brasil.

                Formato:
                {
                  "summaryPt": "string",
                  "evidenceType": "string",
                  "relevanceLevel": "ALTO|MEDIO|BAIXO",
                  "practicalImpact": "string",
                  "warningNote": "string"
                }

                Regras:
                - Não recomende tratamento.
                - Não invente dados.
                - Se o estudo for inconclusivo, diga isso.
                - Seja breve e objetivo.
                - summaryPt deve ter, no máximo, 5 linhas curtas e linguagem simples.
                - Em relevanceLevel, use somente ALTO, MEDIO ou BAIXO.
                - Se não houver dados suficientes, explicite a limitação.
                - Não escreva nada fora do JSON.

                Artigo:
                Título: %s
                Tipo de publicação: %s
                Periódico: %s
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
        return value.substring(0, maxChars) + "... [resumo truncado para análise rápida]";
    }
}
