package com.dev.attendo.repository;

import com.dev.attendo.model.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    @Query("SELECT a FROM ActivityLog a JOIN a.user u JOIN u.role r JOIN u.store s " +
            "WHERE r.name = 'ROLE_ADMIN' " +
            "AND s.id = :storeId " +
            "AND LOWER(u.username) LIKE LOWER(:keyword) " +
            "AND (:actionMethod IS NULL OR a.actionMethod = :actionMethod) " +
            "AND (:startDate IS NULL OR DATE(a.createdDate) >= :startDate) " +
            "AND (:endDate IS NULL OR DATE(a.createdDate) <= :endDate)"
    )
    Page<ActivityLog> getAllActivityLog(Long storeId, String keyword, String actionMethod, LocalDate startDate, LocalDate endDate, Pageable pageDetails);
}
