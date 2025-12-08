package com.dev.attendo.security.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordRequest {
    private String currentPassword;

    @Size(min = 8, max = 30, message = "Password harus memiliki minimal 8 karakter!")
    private String newPassword;
}
