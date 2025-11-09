package com.dev.attendo.service;

import com.dev.attendo.dtos.loan.LoanPagination;
import com.dev.attendo.model.Loan;

import java.time.LocalDate;

public interface LoanService {

    LoanPagination getAllHistoryLoanByUserAndStoreAndMonthYear(Long userId, Long storeId, int month, int year, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);

    Loan getLoanByUserAndStoreAndDate(Long userId, Long storeId, LocalDate expectedDate);

    void addLoan(Long userId, String currentLoggedIn, int newLoanAmount);

    void updateLoan(Long loanId, String currentLoggedIn, int newLoanAmount);

    void removeLoan(Long loanId);
}
