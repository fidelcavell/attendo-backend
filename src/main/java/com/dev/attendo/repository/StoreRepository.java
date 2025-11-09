package com.dev.attendo.repository;

import com.dev.attendo.model.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {

    Optional<Store> findByIdAndIsActive(Long id, boolean isActive);

    @Query("SELECT s FROM Store s WHERE s.owner.username = :username")
    List<Store> findAllByOwnerUsername(String username);
}
