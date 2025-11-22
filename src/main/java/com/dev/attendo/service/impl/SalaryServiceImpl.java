package com.dev.attendo.service.impl;

import com.dev.attendo.exception.BadRequestException;
import com.dev.attendo.exception.InternalServerErrorException;
import com.dev.attendo.exception.ResourceNotFoundException;
import com.dev.attendo.model.Loan;
import com.dev.attendo.model.Salary;
import com.dev.attendo.model.Store;
import com.dev.attendo.model.User;
import com.dev.attendo.repository.*;
import com.dev.attendo.service.ActivityLogService;
import com.dev.attendo.service.SalaryService;
import com.dev.attendo.utils.enums.RoleEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class SalaryServiceImpl implements SalaryService {

    @Autowired
    SalaryRepository salaryRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    StoreRepository storeRepository;

    @Autowired
    AttendanceRepository attendanceRepository;

    @Autowired
    LoanRepository loanRepository;

    @Autowired
    ActivityLogService activityLogService;

    @Override
    public Salary getLatestActiveSalary(Long userId) {
        User selectedUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id: " + userId + " is not found!"));

        return salaryRepository.findLatestActiveSalaryByUserAndOptionalDate(selectedUser.getId(), null)
                .orElseThrow(() -> new ResourceNotFoundException("Salary is not found!"));
    }

    @Override
    public int getCurrentTotalSalary(Long userId, Long storeId, Long loanId) {
        User selectedUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id: " + userId + " is not found!"));

        Store selectedStore = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store with id: " + storeId + " is not found!"));

        // In case of -> User want to update recently loan data due to rules that allow addLoan action can only perform once a day.
        // With this code, currentTotalSalary will show an amount before that loan added.
        Loan selectedLoan = loanRepository.findById(loanId).orElse(null);
        int currentAddedLoan = 0;
        if (selectedLoan != null) {
            currentAddedLoan = selectedLoan.getAmount();
        }

        Salary latestSalary = salaryRepository.findLatestActiveSalaryByUserAndOptionalDate(selectedUser.getId(), null)
                .orElseThrow(() -> new ResourceNotFoundException("Salary is not found!"));

        LocalDate currentDate = LocalDate.now();
        int validAttendancesCount = attendanceRepository.countOnTimeAndLateByYearAndMonth(selectedUser.getId(), currentDate.getMonthValue(), currentDate.getYear()).orElse(0);

        int totalOvertimePay = attendanceRepository.getTotalOvertimePayByYearAndMonth(selectedUser.getId(), currentDate.getMonthValue(), currentDate.getYear()).orElse(0);
        int totalLoan = loanRepository.getTotalLoanByUserAndStoreAndMonthYear(selectedUser.getId(), selectedStore.getId(), currentDate.getMonthValue(), currentDate.getYear()).orElse(0);

        int totalDeduction = attendanceRepository.getTotalDeductionByYearAndMonth(selectedUser.getId(), currentDate.getMonthValue(), currentDate.getYear()).orElse(0);
        int baseSalary = validAttendancesCount * latestSalary.getAmount();

        return baseSalary - (totalLoan + totalDeduction) + currentAddedLoan + totalOvertimePay;
    }

    @Override
    public Map<String, Integer> getMonthlySalarySummaryByUserAndStoreAndMonthYear(Long userId, Long storeId, int month, int year) {
        User selectedUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id: " + userId + " is not found!"));

        Store selectedStore = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store with id: " + storeId + " is not found!"));

        // Get used salary version in that month and year (Perhaps not latest)
        LocalDate targetDate = YearMonth.of(year, month).atEndOfMonth();
        Salary latestSalary = salaryRepository.findLatestActiveSalaryByUserAndOptionalDate(selectedUser.getId(), targetDate)
                .orElseThrow(() -> new ResourceNotFoundException("Salary is not found!"));

        int totalOvertimePay = attendanceRepository.getTotalOvertimePayByYearAndMonth(selectedUser.getId(), month, year).orElse(0);

        int validAttendancesCount = attendanceRepository.countOnTimeAndLateByYearAndMonth(selectedUser.getId(), month, year).orElse(0);

        int totalLoan = loanRepository.getTotalLoanByUserAndStoreAndMonthYear(selectedUser.getId(), selectedStore.getId(), month, year).orElse(0);

        int totalDeduction = attendanceRepository.getTotalDeductionByYearAndMonth(selectedUser.getId(), month, year).orElse(0);
        int baseSalary = validAttendancesCount * latestSalary.getAmount();
        System.out.println("Count : " + validAttendancesCount);

        int totalSalary = baseSalary - (totalLoan + totalDeduction) + totalOvertimePay;

        Map<String, Integer> summary = new HashMap<>();
        summary.put("baseSalary", baseSalary);
        summary.put("totalLoan", totalLoan);
        summary.put("totalDeduction", totalDeduction);
        summary.put("totalOvertimePay", totalOvertimePay);
        summary.put("totalSalary", totalSalary);

        return summary;
    }

    @Transactional
    @Override
    public void addNewSalary(Long userId, String currentLoggedIn, int amount, int targetMonth, int targetYear) {
        User selectedUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id: " + userId + " is not found!"));
        // CHANGE
        User currentUser = userRepository.findByUsernameAndIsActiveTrue(currentLoggedIn)
                .orElseThrow(() -> new ResourceNotFoundException("User with username: " + currentLoggedIn + " is not found!"));

        if (amount <= 0) {
            throw new BadRequestException("Salary amount can't be 0 or less!");
        }

        LocalDate targetDate = (LocalDate.now().getMonthValue() == targetMonth) ? LocalDate.now() : LocalDate.of(targetYear, targetMonth, 1);
        try {
            Salary newSalary = new Salary();
            newSalary.setAmount(amount);
            newSalary.setEffectiveDate(targetDate);
            newSalary.setUser(selectedUser);
            newSalary.setStore(selectedUser.getStore());
            salaryRepository.save(newSalary);

            if (currentUser.getRole().getName() == RoleEnum.ROLE_ADMIN) {
                String activityDescription = currentUser.getUsername() + " is add a new salary with amount of Rp. " + amount + " with effective date on " + Month.of(targetMonth).name().toLowerCase() + " " + targetYear;
                activityLogService.addActivityLog(currentUser, "ADD", "Add new salary", "Salary", activityDescription);
            }

        } catch (Exception e) {
            throw new InternalServerErrorException("Failed to add new salary");
        }
    }
}
