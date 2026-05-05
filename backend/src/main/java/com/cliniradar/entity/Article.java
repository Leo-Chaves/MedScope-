package com.cliniradar.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "article")
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String pubmedId;

    @Column(nullable = false, length = 2000)
    private String title;

    @Lob
    private String abstractText;

    private String journal;

    private LocalDate publishedAt;

    private String publicationDateDisplay;

    private String publicationType;

    @Column(length = 500)
    private String url;

    @Column(length = 64)
    private String contentHash;

    public Article() {
    }

    public Article(String pubmedId, String title, String abstractText, String journal,
                   LocalDate publishedAt, String publicationDateDisplay, String publicationType, String url) {
        this.pubmedId = pubmedId;
        this.title = title;
        this.abstractText = abstractText;
        this.journal = journal;
        this.publishedAt = publishedAt;
        this.publicationDateDisplay = publicationDateDisplay;
        this.publicationType = publicationType;
        this.url = url;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPubmedId() {
        return pubmedId;
    }

    public void setPubmedId(String pubmedId) {
        this.pubmedId = pubmedId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAbstractText() {
        return abstractText;
    }

    public void setAbstractText(String abstractText) {
        this.abstractText = abstractText;
    }

    public String getJournal() {
        return journal;
    }

    public void setJournal(String journal) {
        this.journal = journal;
    }

    public LocalDate getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDate publishedAt) {
        this.publishedAt = publishedAt;
    }

    public String getPublicationDateDisplay() {
        return publicationDateDisplay;
    }

    public void setPublicationDateDisplay(String publicationDateDisplay) {
        this.publicationDateDisplay = publicationDateDisplay;
    }

    public String getPublicationType() {
        return publicationType;
    }

    public void setPublicationType(String publicationType) {
        this.publicationType = publicationType;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }
}
