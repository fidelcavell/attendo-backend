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

    @Size(min = 3, max = 20, message = "Username must be at least 3 character!")
    private String username;

    @NotBlank(message = "Email must not be empty!")
    @Email
    private String email;

    @Getter
    @Setter
    private Set<String> role;

    @Size(min = 8, max = 30, message = "Password must be at least 8 character!")
    private String password;
}
