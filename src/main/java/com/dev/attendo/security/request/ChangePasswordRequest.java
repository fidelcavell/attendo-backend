package com.dev.attendo.security.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordRequest {
    private String currentPassword;

    @Size(min = 8, max = 30, message = "Password must be at least 8 character!")
    private String newPassword;
}
