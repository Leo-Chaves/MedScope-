package com.cliniradar.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "scielo")
public class ScieloProperties {

    private String baseUrl;
    private List<String> collections = List.of("scl", "spa");
    private Integer harvestSize = 20;
    private Integer maxResults = 2;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public List<String> getCollections() {
        return collections;
    }

    public void setCollections(List<String> collections) {
        this.collections = collections;
    }

    public Integer getHarvestSize() {
        return harvestSize;
    }

    public void setHarvestSize(Integer harvestSize) {
        this.harvestSize = harvestSize;
    }

    public Integer getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(Integer maxResults) {
        this.maxResults = maxResults;
    }
}
