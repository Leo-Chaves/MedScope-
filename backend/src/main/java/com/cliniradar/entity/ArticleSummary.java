package com.cliniradar.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "article_summary")
public class ArticleSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id", nullable = false, unique = true)
    private Article article;

    @Column(nullable = false, length = 4000)
    private String summaryPt;

    @Column(nullable = false)
    private String relevanceLevel;

    @Column(nullable = false)
    private String evidenceType;

    @Column(nullable = false, length = 2000)
    private String practicalImpact;

    @Column(nullable = false, length = 2000)
    private String warningNote;

    public ArticleSummary() {
    }

    public ArticleSummary(Article article) {
        this.article = article;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Article getArticle() {
        return article;
    }

    public void setArticle(Article article) {
        this.article = article;
    }

    public String getSummaryPt() {
        return summaryPt;
    }

    public void setSummaryPt(String summaryPt) {
        this.summaryPt = summaryPt;
    }

    public String getRelevanceLevel() {
        return relevanceLevel;
    }

    public void setRelevanceLevel(String relevanceLevel) {
        this.relevanceLevel = relevanceLevel;
    }

    public String getEvidenceType() {
        return evidenceType;
    }

    public void setEvidenceType(String evidenceType) {
        this.evidenceType = evidenceType;
    }

    public String getPracticalImpact() {
        return practicalImpact;
    }

    public void setPracticalImpact(String practicalImpact) {
        this.practicalImpact = practicalImpact;
    }

    public String getWarningNote() {
        return warningNote;
    }

    public void setWarningNote(String warningNote) {
        this.warningNote = warningNote;
    }
}
