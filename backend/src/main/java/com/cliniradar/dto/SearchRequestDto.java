package com.cliniradar.dto;

import jakarta.validation.constraints.NotBlank;

public class SearchRequestDto {

    public static final String SOURCE_BOTH = "BOTH";
    public static final String SOURCE_PUBMED = "PUBMED";
    public static final String SOURCE_SCIELO = "SCIELO";

    @NotBlank(message = "O CID é obrigatório.")
    private String cid;

    private String context;

    private String source = SOURCE_BOTH;

    private boolean continueLoading;

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public boolean isContinueLoading() {
        return continueLoading;
    }

    public void setContinueLoading(boolean continueLoading) {
        this.continueLoading = continueLoading;
    }
}
