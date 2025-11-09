package com.dev.attendo.service;

import com.dev.attendo.model.Salary;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface SalaryService {

    Salary getLatestActiveSalary(Long userId);

    int getCurrentTotalSalary(Long userId, Long storeId, Long loanId);

    List<Salary> getAllSalaryHistoryByStoreAndMonthYear(Long userId, Long storeId, int year, String month);

    Map<String, Integer> getMonthlySalarySummaryByUserAndStoreAndMonthYear(Long userId, Long storeId, int month, int year);

    void addNewSalary(Long userId, String currentLoggedIn, int amount, int targetMonth, int targetYear);
}
