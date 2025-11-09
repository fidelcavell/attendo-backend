package com.dev.attendo.repository;

import com.dev.attendo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameAndIsActiveTrue(String username);

    Optional<User> findByEmailAndIsActiveTrue(String email);

    Boolean existsByUsernameAndIsActiveTrue(String username);

    Boolean existsByEmailAndIsActiveTrue(String email);

    List<User> findAllByStoreIdAndIsActive(Long storeId, boolean isActive);
}
