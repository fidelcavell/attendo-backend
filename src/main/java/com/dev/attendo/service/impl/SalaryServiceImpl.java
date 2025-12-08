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
import java.util.Map;

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
                .orElseThrow(() -> new ResourceNotFoundException("User dengan id: " + userId + " tidak ditemukan!"));

        LocalDate targetDate = YearMonth.of(LocalDate.now().getYear(), LocalDate.now().getMonth()).atEndOfMonth();
        return salaryRepository.findLatestActiveSalaryByUserAndOptionalDate(selectedUser.getId(), targetDate)
                .orElseThrow(() -> new ResourceNotFoundException("Data gaji tidak ditemukan!"));
    }

    @Override
    public int getCurrentTotalSalary(Long userId, Long storeId, Long loanId) {
        User selectedUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User dengan id: " + userId + " tidak ditemukan!"));

        Store selectedStore = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Data toko dengan id: " + storeId + " tidak ditemukan!"));

        // In case of -> User want to update recently loan data due to rules that allow addLoan action can only perform once a day.
        // With this code, currentTotalSalary will show an amount before that loan added.
        Loan selectedLoan = loanRepository.findById(loanId).orElse(null);
        int currentAddedLoan = 0;
        if (selectedLoan != null) {
            currentAddedLoan = selectedLoan.getAmount();
        }

        Salary latestSalary = salaryRepository.findLatestActiveSalaryByUserAndOptionalDate(selectedUser.getId(), null)
                .orElseThrow(() -> new ResourceNotFoundException("Data gaji tidak ditemukan!"));

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
                .orElseThrow(() -> new ResourceNotFoundException("User dengan id: " + userId + " tidak ditemukan!"));

        Store selectedStore = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Data toko dengan id: " + storeId + " tidak ditemukan!"));

        // Get used salary version in that month and year (Perhaps not latest)
        LocalDate targetDate = YearMonth.of(year, month).atEndOfMonth();
        Salary latestSalary = salaryRepository.findLatestActiveSalaryByUserAndOptionalDate(selectedUser.getId(), targetDate)
                .orElseThrow(() -> new ResourceNotFoundException("Data gaji tidak ditemukan!"));

        int totalOvertimePay = attendanceRepository.getTotalOvertimePayByYearAndMonth(selectedUser.getId(), month, year).orElse(0);

        int validAttendancesCount = attendanceRepository.countOnTimeAndLateByYearAndMonth(selectedUser.getId(), month, year).orElse(0);

        int totalLoan = loanRepository.getTotalLoanByUserAndStoreAndMonthYear(selectedUser.getId(), selectedStore.getId(), month, year).orElse(0);

        int totalDeduction = attendanceRepository.getTotalDeductionByYearAndMonth(selectedUser.getId(), month, year).orElse(0);
        int baseSalary = validAttendancesCount * latestSalary.getAmount();

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
                .orElseThrow(() -> new ResourceNotFoundException("User dengan id: " + userId + " tidak ditemukan!"));
        User currentUser = userRepository.findByUsernameAndIsActiveTrue(currentLoggedIn)
                .orElseThrow(() -> new ResourceNotFoundException("User dengan username: " + currentLoggedIn + " tidak ditemukan!"));

        if (amount <= 0) {
            throw new BadRequestException("Jumlah gaji tidak bisa 0 atau negatif!");
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
                String activityDescription = currentUser.getUsername() + " menambahkan data gaji baru dengan jumlah sebesar Rp. " + amount + " dan dengan tanggal berlaku pada " + Month.of(targetMonth).name().toLowerCase() + " " + targetYear;
                activityLogService.addActivityLog(currentUser, "ADD", "Add new salary", "Salary", activityDescription);
            }

        } catch (Exception e) {
            throw new InternalServerErrorException("Gagal menambahkan data gaji baru!");
        }
    }
}
