package com.dev.attendo.security.request;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Data
public class SignUpRequest {

    @Size(min = 3, max = 20, message = "Username harus memiliki minimal 3 karakter!")
    private String username;

    @Email
    @NotBlank(message = "Email tidak boleh kosong!")
    private String email;

    @Getter
    @Setter
    private Set<String> role;

    @Size(min = 8, max = 30, message = "Password harus memiliki minimal 8 karakter!")
    private String password;
}
