package com.dev.attendo.repository;

import com.dev.attendo.model.Salary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SalaryRepository extends JpaRepository<Salary, Long> {

    @Query("SELECT s FROM Salary s WHERE s.user.id = :userId " +
            "AND s.effectiveDate <= COALESCE(:targetDate, CURRENT_DATE) " +
            "ORDER BY s.effectiveDate DESC, s.createdDate DESC LIMIT 1")
    Optional<Salary> findLatestActiveSalaryByUserAndOptionalDate(Long userId, LocalDate targetDate);
}
