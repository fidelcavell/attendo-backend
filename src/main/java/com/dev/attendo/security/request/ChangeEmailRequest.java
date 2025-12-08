package com.dev.attendo.security.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangeEmailRequest {
    @Email
    @NotBlank(message = "Email tidak boleh kosong!")
    private String newEmail;

    private String currentPassword;
}
