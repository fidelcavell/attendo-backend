package com.dev.attendo.controller;

import com.dev.attendo.dtos.loan.LoanPagination;
import com.dev.attendo.security.response.MessageResponse;
import com.dev.attendo.service.LoanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/loan")
@PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
public class LoanController {

    @Autowired
    LoanService loanService;

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/history/{userId}")
    public ResponseEntity<?> getAllLoanHistoryByUserAndStore(
            @PathVariable Long userId,
            @RequestParam(name = "store") Long storeId,
            @RequestParam int month,
            @RequestParam int year,
            @RequestParam(defaultValue = "0", required = false) Integer pageNumber,
            @RequestParam(defaultValue = "5", required = false) Integer pageSize,
            @RequestParam(defaultValue = "createdDate", required = false) String sortBy,
            @RequestParam(defaultValue = "desc", required = false) String sortOrder
    ) {
        LoanPagination loanPagination = loanService.getAllHistoryLoanByUserAndStoreAndMonthYear(userId, storeId, month, year, pageNumber, pageSize, sortBy, sortOrder);
        return ResponseEntity.ok(loanPagination);
    }

    @PostMapping("/{userId}")
    public ResponseEntity<?> addLoan(@PathVariable Long userId, @RequestParam String currentLoggedIn, @RequestParam int newLoanAmount) {
        loanService.addLoan(userId, currentLoggedIn, newLoanAmount);
        return ResponseEntity.ok(new MessageResponse(true, "New loan has been added!"));
    }

    @PutMapping("/{loanId}")
    public ResponseEntity<?> updateLoan(@PathVariable Long loanId, @RequestParam String currentLoggedIn, @RequestParam int newLoanAmount) {
        loanService.updateLoan(loanId, currentLoggedIn, newLoanAmount);
        return ResponseEntity.ok(new MessageResponse(true, "Loan has been updated!"));
    }
}
