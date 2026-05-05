package com.cliniradar.client;

import com.cliniradar.dto.ScientificArticleDto;
import java.util.List;

public interface ScientificArticleSearchClient {

    List<ScientificArticleDto> searchArticles(String query);
}
