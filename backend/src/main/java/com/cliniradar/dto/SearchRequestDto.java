package com.cliniradar.dto;

import jakarta.validation.constraints.NotBlank;

public class SearchRequestDto {

    @NotBlank(message = "O CID é obrigatório.")
    private String cid;

    private String context;

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

    public boolean isContinueLoading() {
        return continueLoading;
    }

    public void setContinueLoading(boolean continueLoading) {
        this.continueLoading = continueLoading;
    }
}
