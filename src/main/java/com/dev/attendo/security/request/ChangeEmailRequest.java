package com.dev.attendo.security.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangeEmailRequest {
    @Email
    @NotBlank
    private String newEmail;

    private String currentPassword;
}
