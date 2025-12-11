package com.dev.attendo.controller;

import com.dev.attendo.security.response.MessageResponse;
import com.dev.attendo.service.SalaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/salary")
@PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
public class SalaryController {

    @Autowired
    SalaryService salaryService;

    @GetMapping("/latest/{userId}")
    public ResponseEntity<?> getLatestActiveSalaryByUser(@PathVariable Long userId) {
       return ResponseEntity.ok(salaryService.getLatestActiveSalary(userId));
    }

    @GetMapping("/current-total-salary/{userId}")
    public ResponseEntity<?> getCurrentTotalSalaryByUser(
            @PathVariable Long userId,
            @RequestParam(name = "store") Long storeId,
            @RequestParam(name = "loan", required = false, defaultValue = "0") Long loanId
    ) {
        return ResponseEntity.ok(salaryService.getCurrentTotalSalary(userId, storeId, loanId));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/monthly-salary-summary/{userId}")
    public ResponseEntity<?> getMonthlySalarySummaryByUserAndStoreAndMonthYear(
            @PathVariable Long userId,
            @RequestParam(name = "store") Long storeId,
            @RequestParam int month,
            @RequestParam int year
    ) {
        return ResponseEntity.ok(salaryService.getMonthlySalarySummaryByUserAndStoreAndMonthYear(userId, storeId, month, year));
    }

    @PostMapping("/add-new-salary/{userId}")
    public ResponseEntity<?> addNewSalary(
            @PathVariable Long userId,
            @RequestParam String currentLoggedIn,
            @RequestParam int salaryAmount,
            @RequestParam int targetMonth,
            @RequestParam int targetYear
    ) {
        salaryService.addNewSalary(userId, currentLoggedIn, salaryAmount, targetMonth, targetYear);
        return ResponseEntity.ok(new MessageResponse(true, "Data gaji berhasil ditambahkan!"));
    }
}
