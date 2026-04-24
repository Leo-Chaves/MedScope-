package com.cliniradar.repository;

import com.cliniradar.entity.Article;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    Optional<Article> findByPubmedId(String pubmedId);
}
