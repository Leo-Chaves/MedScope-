package com.cliniradar.dto;

import jakarta.validation.constraints.NotBlank;

public class SearchRequestDto {

    @NotBlank(message = "O CID é obrigatório.")
    private String cid;

    private String context;

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
}
