package com.dev.attendo.service.impl;

import com.dev.attendo.dtos.user.UserDTO;
import com.dev.attendo.exception.BadRequestException;
import com.dev.attendo.exception.InternalServerErrorException;
import com.dev.attendo.exception.ResourceNotFoundException;
import com.dev.attendo.model.*;
import com.dev.attendo.repository.*;
import com.dev.attendo.security.jwt.JwtUtils;
import com.dev.attendo.security.request.ChangeEmailRequest;
import com.dev.attendo.security.request.ChangePasswordRequest;
import com.dev.attendo.security.request.SignInRequest;
import com.dev.attendo.security.request.SignUpRequest;
import com.dev.attendo.security.response.SignInResponse;
import com.dev.attendo.service.ActivityLogService;
import com.dev.attendo.service.UserService;
import com.dev.attendo.utils.enums.RoleEnum;
import com.dev.attendo.utils.enums.TokenTypeEnum;
import com.dev.attendo.utils.helper.EmailService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class UserServiceImpl implements UserService {

    @Value("${frontend.url}")
    String frontendUrl;

    @Autowired
    JwtUtils jwtUtils;

    @Autowired
    UserRepository userRepository;

    @Autowired
    StoreRepository storeRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    TokenRepository tokenRepository;

    @Autowired
    EmailService emailService;

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    ActivityLogService activityLogService;

    @Autowired
    SalaryRepository salaryRepository;

    @Autowired
    ModelMapper modelMapper;

    @Transactional
    @Override
    public SignInResponse signIn(SignInRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        } catch (AuthenticationException e) {
            throw new ResourceNotFoundException("Bad Credentials!");
        }

        // Set Security Context with authenticated user by using Login Form.
        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String jwtToken = jwtUtils.generateTokenFromUsername(userDetails);
        List<String> roles = userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

        return new SignInResponse(jwtToken, userDetails.getUsername(), roles);
    }

    @Transactional
    @Override
    public void signUp(SignUpRequest request) {
        if (userRepository.existsByUsernameAndIsActiveTrue(request.getUsername())) {
            throw new BadRequestException("Username is already taken!");
        }

        if (userRepository.existsByEmailAndIsActiveTrue(request.getEmail())) {
            throw new BadRequestException("Email is already taken!");
        }

        User selectedUser = userRepository.findByUsername(request.getUsername()).orElse(null);
        Set<String> requestRole = request.getRole();
        Role role;

        if (requestRole == null || requestRole.isEmpty()) {
            role = roleRepository.findByName(RoleEnum.ROLE_EMPLOYEE)
                    .orElseThrow(() -> new ResourceNotFoundException("Role is not found!"));
        } else {
            String existRoles = requestRole.iterator().next();
            if (existRoles.equals("owner")) {
                role = roleRepository.findByName(RoleEnum.ROLE_OWNER)
                        .orElseThrow(() -> new ResourceNotFoundException("Role is not found!"));
            } else {
                role = roleRepository.findByName(RoleEnum.ROLE_EMPLOYEE)
                        .orElseThrow(() -> new ResourceNotFoundException("Role is not found!"));
            }
        }

        try {
            if (selectedUser == null) {
                selectedUser = new User(
                        request.getUsername(),
                        request.getEmail(),
                        passwordEncoder.encode(request.getPassword()),
                        role,
                        false
                );
            } else {
                selectedUser.setUsername(request.getUsername());
                selectedUser.setEmail(request.getEmail());
                selectedUser.setPassword(passwordEncoder.encode(request.getPassword()));
                selectedUser.setRole(role);
                selectedUser.setUpdatedDate(LocalDateTime.now());
            }
            userRepository.save(selectedUser);

            String token = UUID.randomUUID().toString();
            Instant expireDate = Instant.now().plus(30, ChronoUnit.MINUTES);
            Token emailVerificationToken = new Token(token, expireDate, selectedUser);
            emailVerificationToken.setType(TokenTypeEnum.EMAIL_VERIFICATION);
            tokenRepository.save(emailVerificationToken);

            String emailVerificationUrl = frontendUrl + "/email-verification?token=" + token;
            String content = "We received a request to activate your account. Click the button below to set a new one:";
            emailService.sendPasswordResetEmail(selectedUser.getEmail(), emailVerificationUrl, "Email Verification", emailVerificationToken.getUser().getUsername(), content, "Verify Email", "30 minutes");

        } catch (Exception e) {
            throw new InternalServerErrorException("Failed to Sign Up!");
        }
    }

    @Transactional
    @Override
    public void generatePasswordResetToken(String email) {
        User selectedUser = userRepository.findByEmailAndIsActiveTrue(email)
                .orElseThrow(() -> new ResourceNotFoundException("User with email: " + email + " is not found!"));

        try {
            String token = UUID.randomUUID().toString();
            Instant expireDate = Instant.now().plus(30, ChronoUnit.MINUTES);
            Token resetToken = new Token(token, expireDate, selectedUser);
            resetToken.setType(TokenTypeEnum.RESET_PASSWORD);
            tokenRepository.save(resetToken);

            String resetPasswordUrl = frontendUrl + "/reset-password?token=" + token;
            String content = "We received a request to reset your password. Click the button below to set a new one:";
            emailService.sendPasswordResetEmail(selectedUser.getEmail(), resetPasswordUrl, "Password Reset Request", resetToken.getUser().getUsername(), content, "Reset Password", "30 minutes");

        } catch (Exception e) {
            throw new InternalServerErrorException("Failed to sent the email! " + e);
        }
    }

    @Transactional
    @Override
    public void resetPassword(String token, String newPassword) {
        Token resetToken = tokenRepository.findByTokenAndType(token, TokenTypeEnum.RESET_PASSWORD)
                .orElseThrow(() -> new ResourceNotFoundException("Token is not found!"));

        if (resetToken.isUsed()) {
            throw new BadRequestException("Token has already been used!");
        }
        if (resetToken.getExpireDate().isBefore(Instant.now())) {
            throw new BadRequestException("Token has expired!");
        }

        try {
            User selectedUser = resetToken.getUser();
            selectedUser.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(selectedUser);

            resetToken.setUsed(true);
            tokenRepository.save(resetToken);

        } catch (Exception e) {
            throw new InternalServerErrorException("Failed to reset the password!");
        }
    }

    @Transactional
    @Override
    public void addEmployeeToStore(Long storeId, String username, String currentLoggedInUser, int salaryAmount) {
        User selectedUser = userRepository.findByUsernameAndIsActiveTrue(username)
                .orElseThrow(() -> new ResourceNotFoundException("User with username: " + username + " is not found!"));
        Store selectedStore = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store with id: " + storeId + " is not found!"));
        User loggedInUser = userRepository.findByUsernameAndIsActiveTrue(currentLoggedInUser)
                .orElseThrow(() -> new ResourceNotFoundException("User with username: " + currentLoggedInUser + " is not found!"));

        if (selectedUser.getRole().getName() == RoleEnum.ROLE_OWNER) {
            throw new BadRequestException("User with username: " + username + "can't be added (OWNER)!");
        }

        if (selectedUser.getStore() != null) {
            if (!selectedUser.getStore().getId().equals(storeId)) {
                throw new BadRequestException("User with username: " + username + " is already associated with another store!");
            }
            throw new BadRequestException("User with username: " + username + " is already part of this store!");
        }

        if (!selectedStore.isActive()) {
            throw new BadRequestException("Your store is deactivate, cannot add new employee!");
        }

        try {
            if (loggedInUser.getRole().getName() == RoleEnum.ROLE_ADMIN) {
                activityLogService.addActivityLog(loggedInUser, "ADD", "Add new employee to Store", "User", "Add new employee: " + username + " to store: " + selectedStore.getName() + " with salary amount: Rp." + salaryAmount);
            }

            selectedUser.setStore(selectedStore);
            userRepository.save(selectedUser);

            Salary newSalary = new Salary();
            newSalary.setAmount(salaryAmount);
            newSalary.setEffectiveDate(LocalDate.now());
            newSalary.setUser(selectedUser);
            newSalary.setStore(selectedStore);
            salaryRepository.save(newSalary);

        } catch (Exception e) {
            throw new InternalServerErrorException("Failed to remove the employee!");
        }
    }

    @Transactional
    @Override
    public void removeEmployeeFromStore(String username) {
        User user = userRepository.findByUsernameAndIsActiveTrue(username)
                .orElseThrow(() -> new ResourceNotFoundException("User with username: " + username + " is not found!"));

        try {
            user.getProfile().setSchedule(null);
            user.setStore(null);
            user.setUpdatedDate(LocalDateTime.now());
            userRepository.save(user);

        } catch (Exception e) {
            throw new InternalServerErrorException("Failed to remove the employee!");
        }
    }

    @Override
    public void updateEmployeeRole(String username) {
        User user = userRepository.findByUsernameAndIsActiveTrue(username)
                .orElseThrow(() -> new ResourceNotFoundException("User with username: " + username + " is not found!"));
        Role employeeRole = roleRepository.findByName(RoleEnum.ROLE_EMPLOYEE)
                .orElseThrow(() -> new ResourceNotFoundException("Role is not found!"));
        Role adminRole = roleRepository.findByName(RoleEnum.ROLE_ADMIN)
                .orElseThrow(() -> new ResourceNotFoundException("Role is not found!"));

        try {
            if (user.getRole().getName().equals(RoleEnum.ROLE_EMPLOYEE)) {
                user.setRole(adminRole);
            } else {
                user.setRole(employeeRole);
            }
            userRepository.save(user);

        } catch (Exception e) {
            throw new InternalServerErrorException("Failed to update employee's role!");
        }
    }

    @Override
    public UserDTO getUserByUsername(String username) {
        User selectedUser = userRepository.findByUsernameAndIsActiveTrue(username)
                .orElseThrow(() -> new ResourceNotFoundException("User with username: " + username + " is not found!"));
        UserDTO userDTO = modelMapper.map(selectedUser, UserDTO.class);
        userDTO.setIdProfile(selectedUser.getProfile() == null ? null : selectedUser.getProfile().getId());
        userDTO.setIdSchedule(selectedUser.getProfile() == null ? null : selectedUser.getProfile().getSchedule() == null ? null : selectedUser.getProfile().getSchedule().getId());
        userDTO.setIdStore(selectedUser.getStore() == null ? null : selectedUser.getStore().getId());
        return userDTO;
    }

    @Transactional
    @Override
    public void changePassword(String username, ChangePasswordRequest request) {
        User selectedUser = userRepository.findByUsernameAndIsActiveTrue(username)
                .orElseThrow(() -> new ResourceNotFoundException("User with username: " + username + " is not found!"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), selectedUser.getPassword())) {
            throw new BadRequestException("Your current password is incorrect!");
        }
        selectedUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(selectedUser);
    }

    @Transactional
    @Override
    public void deleteAccount(String username) {
        // CHANGE
        User selectedUser = userRepository.findByUsernameAndIsActiveTrue(username)
                .orElseThrow(() -> new ResourceNotFoundException("User with username: " + username + " is not found!"));

        try {
            String base = LocalDateTime.now().toString() + UUID.randomUUID();
            int hash = Math.abs(base.hashCode()) % 1_000_000;
            String uniqueCode = String.format("%06d", hash);

            selectedUser.setUsername(selectedUser.getUsername().trim() + "-" + uniqueCode);
            selectedUser.setEmail(selectedUser.getEmail().trim() + "-" + uniqueCode);
            selectedUser.setActive(!selectedUser.isActive());
            userRepository.save(selectedUser);

        } catch (Exception e) {
            throw new InternalServerErrorException("Failed to delete account!");
        }
    }

    @Override
    public void emailVerification(String token, String type) {
        TokenTypeEnum tokenType = TokenTypeEnum.valueOf(type);

        Token emailVerifyToken = tokenRepository.findByTokenAndType(token, tokenType)
                .orElseThrow(() -> new ResourceNotFoundException("Token is not found!"));

        if (emailVerifyToken.isUsed()) {
            throw new BadRequestException("Token has already been used!");
        }
        if (emailVerifyToken.getExpireDate().isBefore(Instant.now())) {
            throw new BadRequestException("Token has expired!");
        }
        if (emailVerifyToken.getUser().isActive() && tokenType == TokenTypeEnum.EMAIL_VERIFICATION) {
            throw new InternalServerErrorException("Account is already activated!");
        }

        try {
            User selectedUser = emailVerifyToken.getUser();

            if (tokenType == TokenTypeEnum.EMAIL_VERIFICATION) {
                selectedUser.setActive(true);
            } else  {
                selectedUser.setEmail(emailVerifyToken.getNewEmail());
            }
            userRepository.save(selectedUser);

            emailVerifyToken.setUsed(true);
            tokenRepository.save(emailVerifyToken);

        } catch (Exception e) {
            throw new InternalServerErrorException("Failed to verify email");
        }
    }

    @Override
    public void changeEmail(String username, ChangeEmailRequest request) {
        // CHANGE
        User selectedUser = userRepository.findByUsernameAndIsActiveTrue(username)
                .orElseThrow(() -> new ResourceNotFoundException("User with username: " + username + " is not found!"));

        User existingUser = userRepository.findByEmailAndIsActiveTrue(request.getNewEmail()).orElse(null);

        if (!passwordEncoder.matches(request.getCurrentPassword(), selectedUser.getPassword())) {
            throw new BadRequestException("Your current password is incorrect!");
        }

        if (Objects.equals(selectedUser.getEmail(), request.getNewEmail()) || existingUser != null) {
            throw new BadRequestException("Email is already taken!");
        }

        try {
            String token = UUID.randomUUID().toString();
            Instant expireDate = Instant.now().plus(30, ChronoUnit.MINUTES);

            Token changeEmailToken = new Token(token, expireDate, selectedUser);
            changeEmailToken.setNewEmail(request.getNewEmail());
            changeEmailToken.setType(TokenTypeEnum.EMAIL_CHANGE);
            tokenRepository.save(changeEmailToken);

            String changeEmailUrl = frontendUrl + "/verify-email-change?token=" + token;
            String content = "We received a request to change your account's email to " + request.getNewEmail() + ". Click the button below to set a change your account's email:";
            emailService.sendPasswordResetEmail(request.getNewEmail(), changeEmailUrl, "Change Email Request", changeEmailToken.getUser().getUsername(), content, "Change Email", "30 minutes");

        } catch (Exception e) {
            throw new InternalServerErrorException("Failed to sent the email! " + e);
        }
    }
}
