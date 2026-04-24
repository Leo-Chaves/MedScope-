package com.cliniradar.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class CidMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String cidCode;

    @Column(nullable = false)
    private String displayName;

    @Column(nullable = false)
    private String englishQueryBase;

    private String area;

    public CidMapping() {
    }

    public CidMapping(String cidCode, String displayName, String englishQueryBase, String area) {
        this.cidCode = cidCode;
        this.displayName = displayName;
        this.englishQueryBase = englishQueryBase;
        this.area = area;
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

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEnglishQueryBase() {
        return englishQueryBase;
    }

    public void setEnglishQueryBase(String englishQueryBase) {
        this.englishQueryBase = englishQueryBase;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }
}
