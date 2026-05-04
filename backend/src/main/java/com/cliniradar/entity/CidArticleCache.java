package com.cliniradar.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "cid_article_cache",
        uniqueConstraints = @UniqueConstraint(columnNames = {"cid_code", "cache_position"})
)
public class CidArticleCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cid_code", nullable = false, length = 20)
    private String cidCode;

    @Column(name = "cache_position", nullable = false)
    private Integer position;

    @Column(nullable = false, length = 1000)
    private String queryUsed;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @Column(nullable = false)
    private LocalDateTime refreshedAt;

    public CidArticleCache() {
    }

    public CidArticleCache(String cidCode, Integer position) {
        this.cidCode = cidCode;
        this.position = position;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCidCode() {
        return cidCode;
    }

    public void setCidCode(String cidCode) {
        this.cidCode = cidCode;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public String getQueryUsed() {
        return queryUsed;
    }

    public void setQueryUsed(String queryUsed) {
        this.queryUsed = queryUsed;
    }

    public Article getArticle() {
        return article;
    }

    public void setArticle(Article article) {
        this.article = article;
    }

    public LocalDateTime getRefreshedAt() {
        return refreshedAt;
    }

    public void setRefreshedAt(LocalDateTime refreshedAt) {
        this.refreshedAt = refreshedAt;
    }
}
