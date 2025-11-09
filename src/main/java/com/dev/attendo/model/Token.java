package com.dev.attendo.model;

import com.dev.attendo.utils.enums.TokenTypeEnum;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
public class Token {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private String token;

    @Column(nullable = false)
    private Instant expireDate;

    private boolean used;

    @Email
    private String newEmail;

    @Enumerated(EnumType.STRING)
    private TokenTypeEnum type;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    @JsonBackReference
    @JoinColumn(name = "id_user", referencedColumnName = "id", nullable = false)
    private User user;

    public Token(String token, Instant expireDate, User user) {
        this.token = token;
        this.expireDate = expireDate;
        this.user = user;
    }
}
