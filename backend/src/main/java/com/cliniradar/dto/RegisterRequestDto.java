package com.cliniradar.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterRequestDto {

    @NotBlank(message = "O nome e obrigatorio.")
    private String name;

    @Email(message = "Informe um e-mail valido.")
    @NotBlank(message = "O e-mail e obrigatorio.")
    private String email;

    @Pattern(regexp = "\\d{4,6}/[A-Z]{2}", message = "O CRM deve seguir o formato NNNNNN/UF.")
    @NotBlank(message = "O CRM e obrigatorio.")
    private String crm;

    @Size(min = 6, message = "A senha deve ter no minimo 6 caracteres.")
    @NotBlank(message = "A senha e obrigatoria.")
    private String password;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCrm() {
        return crm;
    }

    public void setCrm(String crm) {
        this.crm = crm;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
