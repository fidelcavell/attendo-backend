package com.dev.attendo.service;

import com.dev.attendo.dtos.user.UserDTO;
import com.dev.attendo.model.User;
import com.dev.attendo.security.request.ChangeEmailRequest;
import com.dev.attendo.security.request.ChangePasswordRequest;
import com.dev.attendo.security.request.SignInRequest;
import com.dev.attendo.security.request.SignUpRequest;
import com.dev.attendo.security.response.SignInResponse;

public interface UserService {

    SignInResponse signIn(SignInRequest request);

    void signUp(SignUpRequest request);

    void generatePasswordResetToken(String email);

    void resetPassword(String token, String newPassword);

    void addEmployeeToStore(Long storeId, String currentUser, String username, int salaryAmount);

    void removeEmployeeFromStore(String username);

    void updateEmployeeRole(String username);

    UserDTO getUserByUsername(String username);

    void changePassword(String username, ChangePasswordRequest request);

    void deleteAccount(String username);

    void emailVerification(String token, String type);

    void changeEmail(String username, ChangeEmailRequest request);
}
