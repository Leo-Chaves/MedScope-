package com.cliniradar.repository;

import com.cliniradar.entity.CidArticleCache;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CidArticleCacheRepository extends JpaRepository<CidArticleCache, Long> {

    List<CidArticleCache> findByCidCodeOrderByPositionAsc(String cidCode);

    Optional<CidArticleCache> findByCidCodeAndPosition(String cidCode, Integer position);
}
