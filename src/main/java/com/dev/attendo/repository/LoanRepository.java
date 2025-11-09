package com.dev.attendo.repository;

import com.dev.attendo.model.Loan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {

    @Query("SELECT l FROM Loan l " +
            "WHERE l.user.id = :userId " +
            "AND l.store.id = :storeId " +
            "AND YEAR(l.createdDate) = :year " +
            "AND MONTH(l.createdDate) = :month")
    Page<Loan> getAllLoanHistoryByUserAndStoreAndMonthYear(Long userId, Long storeId, int month, int year, Pageable pageDetails);

    @Query("SELECT SUM(l.amount) FROM Loan l WHERE l.user.id = :userId " +
            "AND l.store.id = :storeId " +
            "AND MONTH(l.createdDate) = :month " +
            "AND YEAR(l.createdDate) = :year")
    Optional<Integer> getTotalLoanByUserAndStoreAndMonthYear(Long userId, Long storeId, int month, int year);

    @Query("SELECT l FROM Loan l WHERE l.user.id = :userId " +
            "AND l.store.id = :storeId " +
            "AND FUNCTION('DATE', l.createdDate) = :expectedDate")
    Optional<Loan> getLoanByUserAndStoreAndDate(Long userId, Long storeId, LocalDate expectedDate);
}
