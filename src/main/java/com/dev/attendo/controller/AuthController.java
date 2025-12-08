package com.dev.attendo.controller;

import com.dev.attendo.security.request.ChangeEmailRequest;
import com.dev.attendo.security.request.ChangePasswordRequest;
import com.dev.attendo.security.request.SignInRequest;
import com.dev.attendo.security.request.SignUpRequest;
import com.dev.attendo.security.response.MessageResponse;
import com.dev.attendo.security.response.SignInResponse;
import com.dev.attendo.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    UserService userService;

    @PostMapping("/sign-in")
    public ResponseEntity<?> signIn(@RequestBody SignInRequest request) {
        SignInResponse response = userService.signIn(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sign-up")
    public ResponseEntity<?> signUp(@Valid @RequestBody SignUpRequest request) {
        userService.signUp(request);
        return ResponseEntity.ok(new MessageResponse(true, "Email verifikasi berhasil dikirim!"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestParam String email) {
        userService.generatePasswordResetToken(email);
        return ResponseEntity.ok(new MessageResponse(true, "Password Reset Token berhasil dikirim!"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestParam String token, @RequestParam String newPassword) {
        userService.resetPassword(token, newPassword);
        return ResponseEntity.ok(new MessageResponse(true, "Password berhasil diubah!"));
    }

    @PutMapping("/account-email-activation")
    public ResponseEntity<?> accountEmailActivation(@RequestParam String token) {
        userService.emailVerification(token, "EMAIL_VERIFICATION");
        return ResponseEntity.ok(new MessageResponse(true, "Email berhasil diverifikasi!"));
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/change-password/{username}")
    public ResponseEntity<?> changePassword(@PathVariable String username, @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(username, request);
        return ResponseEntity.ok(new MessageResponse(true, "Password berhasil diubah!"));
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/change-email/{username}")
    public ResponseEntity<?> changeEmail(@PathVariable String username, @Valid @RequestBody ChangeEmailRequest request) {
        userService.changeEmail(username, request);
        return ResponseEntity.ok(new MessageResponse(true, "Verifikasi ubah email berhasil dikirim!"));
    }

    @PutMapping("/account-email-change")
    public ResponseEntity<?> accountEmailChange(@RequestParam String token) {
        userService.emailVerification(token, "EMAIL_CHANGE");
        return ResponseEntity.ok(new MessageResponse(true, "Email berhasil diubah!"));
    }
}
