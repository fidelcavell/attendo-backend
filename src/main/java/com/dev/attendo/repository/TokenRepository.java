package com.dev.attendo.repository;

import com.dev.attendo.model.Token;
import com.dev.attendo.utils.enums.TokenTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TokenRepository extends JpaRepository<Token, Long> {
    Optional<Token> findByTokenAndType(String token, TokenTypeEnum type);
}
