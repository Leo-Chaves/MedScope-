package com.cliniradar.repository;

import com.cliniradar.entity.Article;
import com.cliniradar.entity.ArticleSummary;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleSummaryRepository extends JpaRepository<ArticleSummary, Long> {

    Optional<ArticleSummary> findByArticle(Article article);
}
