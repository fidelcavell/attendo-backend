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
            throw new BadRequestException("Username telah digunakan!");
        }

        if (userRepository.existsByEmailAndIsActiveTrue(request.getEmail())) {
            throw new BadRequestException("Email telah digunakan!");
        }

        User selectedUser = userRepository.findByUsername(request.getUsername()).orElse(null);
        Set<String> requestRole = request.getRole();
        Role role;

        if (requestRole == null || requestRole.isEmpty()) {
            role = roleRepository.findByName(RoleEnum.ROLE_EMPLOYEE)
                    .orElseThrow(() -> new ResourceNotFoundException("Role tidak ditemukan!"));
        } else {
            String existRoles = requestRole.iterator().next();
            if (existRoles.equals("owner")) {
                role = roleRepository.findByName(RoleEnum.ROLE_OWNER)
                        .orElseThrow(() -> new ResourceNotFoundException("Role tidak ditemukan!"));
            } else {
                role = roleRepository.findByName(RoleEnum.ROLE_EMPLOYEE)
                        .orElseThrow(() -> new ResourceNotFoundException("Role tidak ditemukan!"));
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
            throw new InternalServerErrorException("Gagal melakukan sign up!");
        }
    }

    @Transactional
    @Override
    public void generatePasswordResetToken(String email) {
        User selectedUser = userRepository.findByEmailAndIsActiveTrue(email)
                .orElseThrow(() -> new ResourceNotFoundException("User dengan email: " + email + " tidak ditemukan!"));

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
            throw new InternalServerErrorException("Gagal mengirimkan email!");
        }
    }

    @Transactional
    @Override
    public void resetPassword(String token, String newPassword) {
        Token selectedResetToken = tokenRepository.findByTokenAndType(token, TokenTypeEnum.RESET_PASSWORD)
                .orElseThrow(() -> new ResourceNotFoundException("Token tidak ditemukan!"));

        if (selectedResetToken.isUsed()) {
            throw new BadRequestException("Token telah digunakan!");
        }
        if (selectedResetToken.getExpireDate().isBefore(Instant.now())) {
            throw new BadRequestException("Token telah kadaluarsa!");
        }

        try {
            User selectedUser = selectedResetToken.getUser();
            selectedUser.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(selectedUser);

            selectedResetToken.setUsed(true);
            tokenRepository.save(selectedResetToken);

        } catch (Exception e) {
            throw new InternalServerErrorException("Gagal melakukan reset password!");
        }
    }

    @Transactional
    @Override
    public void addEmployeeToStore(Long storeId, String username, String currentLoggedInUser, int salaryAmount) {
        User selectedUser = userRepository.findByUsernameAndIsActiveTrue(username)
                .orElseThrow(() -> new ResourceNotFoundException("User dengan username: " + username + " tidak ditemukan!"));
        Store selectedStore = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Data toko dengan id: " + storeId + " tidak ditemukan!"));
        User loggedInUser = userRepository.findByUsernameAndIsActiveTrue(currentLoggedInUser)
                .orElseThrow(() -> new ResourceNotFoundException("User dengan username: " + currentLoggedInUser + " tidak ditemukan!"));

        if (selectedUser.getRole().getName() == RoleEnum.ROLE_OWNER) {
            throw new BadRequestException("User dengan username: " + username + " tidak bisa ditambahkan (OWNER)!");
        }

        if (selectedUser.getStore() != null) {
            if (!selectedUser.getStore().getId().equals(storeId)) {
                throw new BadRequestException("User dengan username: " + username + " telah terasosiasi dengan toko lain!");
            }
            throw new BadRequestException("User dengan username: " + username + " telah terasosiasi dengan toko ini!");
        }

        if (!selectedStore.isActive()) {
            throw new BadRequestException("Status toko saat ini adalah deactivate, tidak bisa menambahkan karyawan baru!");
        }

        try {
            selectedUser.setStore(selectedStore);
            userRepository.save(selectedUser);

            Salary newSalary = new Salary();
            newSalary.setAmount(salaryAmount);
            newSalary.setEffectiveDate(LocalDate.now());
            newSalary.setUser(selectedUser);
            newSalary.setStore(selectedStore);
            salaryRepository.save(newSalary);

            if (loggedInUser.getRole().getName() == RoleEnum.ROLE_ADMIN) {
                String activityDescription = "Menambahkan data karyawan baru dengan username: " + username + " ke dalam toko: " + selectedStore.getName() + " dengan jumlah gaji sebesar Rp." + salaryAmount;
                activityLogService.addActivityLog(loggedInUser, "ADD", "Add new employee to Store", "User", activityDescription);
            }

        } catch (Exception e) {
            throw new InternalServerErrorException("Gagal menambahkan data karyawan baru ke toko!");
        }
    }

    @Transactional
    @Override
    public void removeEmployeeFromStore(String username) {
        User user = userRepository.findByUsernameAndIsActiveTrue(username)
                .orElseThrow(() -> new ResourceNotFoundException("User dengan username: " + username + " tidak ditemukan!"));
        Role role = roleRepository.findByName(RoleEnum.ROLE_EMPLOYEE)
                .orElseThrow(() -> new ResourceNotFoundException("Role tidak ditemukan!"));

        try {
            user.getProfile().setSchedule(null);
            user.setStore(null);
            user.setRole(role);
            user.setUpdatedDate(LocalDateTime.now());
            userRepository.save(user);

        } catch (Exception e) {
            throw new InternalServerErrorException("Gagal menghapus data karyawan!");
        }
    }

    @Override
    public void updateEmployeeRole(String username) {
        User user = userRepository.findByUsernameAndIsActiveTrue(username)
                .orElseThrow(() -> new ResourceNotFoundException("User dengan username: " + username + " tidak ditemukan!"));
        Role employeeRole = roleRepository.findByName(RoleEnum.ROLE_EMPLOYEE)
                .orElseThrow(() -> new ResourceNotFoundException("Role tidak ditemukan!"));
        Role adminRole = roleRepository.findByName(RoleEnum.ROLE_ADMIN)
                .orElseThrow(() -> new ResourceNotFoundException("Role tidak ditemukan!"));

        try {
            if (user.getRole().getName().equals(RoleEnum.ROLE_EMPLOYEE)) {
                user.setRole(adminRole);
            } else {
                user.setRole(employeeRole);
            }
            userRepository.save(user);

        } catch (Exception e) {
            throw new InternalServerErrorException("Gagal mengubah hak akses karyawan!");
        }
    }

    @Override
    public UserDTO getUserByUsername(String username) {
        User selectedUser = userRepository.findByUsernameAndIsActiveTrue(username)
                .orElseThrow(() -> new ResourceNotFoundException("User dengan username: " + username + " tidak ditemukan!"));
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
                .orElseThrow(() -> new ResourceNotFoundException("User dengan username: " + username + " tidak ditemukan!"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), selectedUser.getPassword())) {
            throw new BadRequestException("Password saat ini tidak sesuai!");
        }
        selectedUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(selectedUser);
    }

    @Transactional
    @Override
    public void deleteAccount(String username) {
        User selectedUser = userRepository.findByUsernameAndIsActiveTrue(username)
                .orElseThrow(() -> new ResourceNotFoundException("User dengan username: " + username + " tidak ditemukan!"));

        try {
            String base = LocalDateTime.now().toString() + UUID.randomUUID();
            int hash = Math.abs(base.hashCode()) % 1_000_000;
            String uniqueCode = String.format("%06d", hash);

            selectedUser.setUsername(selectedUser.getUsername().trim() + "-" + uniqueCode);
            selectedUser.setEmail(selectedUser.getEmail().trim() + "-" + uniqueCode);
            selectedUser.setActive(!selectedUser.isActive());
            userRepository.save(selectedUser);

        } catch (Exception e) {
            throw new InternalServerErrorException("Gagal menghapus akun karyawan!");
        }
    }

    @Override
    public void emailVerification(String token, String type) {
        TokenTypeEnum tokenType = TokenTypeEnum.valueOf(type);
        Token emailVerifyToken = tokenRepository.findByTokenAndType(token, tokenType)
                .orElseThrow(() -> new ResourceNotFoundException("Token tidak ditemukan!"));

        if (emailVerifyToken.isUsed()) {
            throw new BadRequestException("Token telah digunakan!");
        }
        if (emailVerifyToken.getExpireDate().isBefore(Instant.now())) {
            throw new BadRequestException("Token telah kadaluarsa!");
        }
        if (emailVerifyToken.getUser().isActive() && tokenType == TokenTypeEnum.EMAIL_VERIFICATION) {
            throw new InternalServerErrorException("Akun telah teraktivasi!");
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
            throw new InternalServerErrorException("Gagal memverifikasi email");
        }
    }

    @Override
    public void changeEmail(String username, ChangeEmailRequest request) {
        User selectedUser = userRepository.findByUsernameAndIsActiveTrue(username)
                .orElseThrow(() -> new ResourceNotFoundException("User dengan username: " + username + " tidak ditemukan!"));
        User existingUser = userRepository.findByEmailAndIsActiveTrue(request.getNewEmail()).orElse(null);

        if (!passwordEncoder.matches(request.getCurrentPassword(), selectedUser.getPassword())) {
            throw new BadRequestException("Password saat ini tidak sesuai!");
        }

        if (Objects.equals(selectedUser.getEmail(), request.getNewEmail()) || existingUser != null) {
            throw new BadRequestException("Email telah digunakan!");
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
            throw new InternalServerErrorException("Gagal mengirimkan email!");
        }
    }
}
