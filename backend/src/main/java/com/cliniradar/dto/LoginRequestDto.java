package com.cliniradar.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoginRequestDto {

    @Email(message = "Informe um e-mail valido.")
    @NotBlank(message = "O e-mail e obrigatorio.")
    private String email;

    @Size(min = 6, message = "A senha deve ter no minimo 6 caracteres.")
    @NotBlank(message = "A senha e obrigatoria.")
    private String password;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
