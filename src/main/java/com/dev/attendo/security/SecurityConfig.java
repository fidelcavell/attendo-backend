package com.dev.attendo.security;

import com.dev.attendo.model.Role;
import com.dev.attendo.model.Store;
import com.dev.attendo.model.User;
import com.dev.attendo.repository.RoleRepository;
import com.dev.attendo.repository.StoreRepository;
import com.dev.attendo.repository.UserRepository;
import com.dev.attendo.security.jwt.AuthEntryPointJwt;
import com.dev.attendo.security.jwt.AuthTokenFilter;
import com.dev.attendo.utils.enums.RoleEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private AuthEntryPointJwt unAuthorizedHandler;

    @Bean
    public AuthTokenFilter authJwtTokenFilter() {
        return new AuthTokenFilter();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable);
        http.authorizeHttpRequests((requests) -> requests
                .requestMatchers("/api/csrf-token").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .anyRequest().authenticated()
        );
        http.exceptionHandling(exception -> exception.authenticationEntryPoint(unAuthorizedHandler));
        http.addFilterBefore(authJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    // Initial Data
    @Bean
    public CommandLineRunner initialData(RoleRepository roleRepository, UserRepository userRepository, StoreRepository storeRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            Role ownerRole = roleRepository.findByName(RoleEnum.ROLE_OWNER)
                    .orElseGet(() -> roleRepository.save(new Role(RoleEnum.ROLE_OWNER)));
            Role adminRole = roleRepository.findByName(RoleEnum.ROLE_ADMIN)
                    .orElseGet(() -> roleRepository.save(new Role(RoleEnum.ROLE_ADMIN)));
            Role employeeRole = roleRepository.findByName(RoleEnum.ROLE_EMPLOYEE)
                    .orElseGet(() -> roleRepository.save(new Role(RoleEnum.ROLE_EMPLOYEE)));

            if (!userRepository.existsByUsernameAndIsActiveTrue("owner1")) {
                User owner1 = new User("owner1", "owner1@example.com", passwordEncoder.encode("owner1"), ownerRole, true);
                userRepository.save(owner1);
            }
            if (!userRepository.existsByUsernameAndIsActiveTrue("employee1")) {
                User employee1 = new User("employee1", "employee1@example.com", passwordEncoder.encode("employee1"), adminRole, true);
                userRepository.save(employee1);
            }
            if (!userRepository.existsByUsernameAndIsActiveTrue("employee2")) {
                User employee2 = new User("employee2", "employee2@example.com", passwordEncoder.encode("employee2"), employeeRole, true);
                userRepository.save(employee2);
            }
            if (!userRepository.existsByUsernameAndIsActiveTrue("employee3")) {
                User employee3 = new User("employee3", "employee3@example.com", passwordEncoder.encode("employee3"), employeeRole, true);
                userRepository.save(employee3);
            }
        };
    }
}
