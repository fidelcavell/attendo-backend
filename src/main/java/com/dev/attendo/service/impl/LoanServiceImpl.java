package com.dev.attendo.service.impl;

import com.dev.attendo.dtos.loan.LoanDTO;
import com.dev.attendo.dtos.loan.LoanPagination;
import com.dev.attendo.exception.BadRequestException;
import com.dev.attendo.exception.InternalServerErrorException;
import com.dev.attendo.exception.ResourceNotFoundException;
import com.dev.attendo.model.*;
import com.dev.attendo.repository.*;
import com.dev.attendo.service.ActivityLogService;
import com.dev.attendo.service.LoanService;
import com.dev.attendo.utils.enums.RoleEnum;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class LoanServiceImpl implements LoanService {

    @Autowired
    LoanRepository loanRepository;

    @Autowired
    AttendanceRepository attendanceRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    SalaryRepository salaryRepository;

    @Autowired
    StoreRepository storeRepository;

    @Autowired
    ActivityLogService activityLogService;

    @Autowired
    ModelMapper modelMapper;

    @Override
    public LoanPagination getAllHistoryLoanByUserAndStoreAndMonthYear(Long userId, Long storeId, int month, int year, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        User selectedUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id: " + userId + " is not found!"));

        Store selectedStore = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store with id: " + storeId + " is not found!"));

        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Page<Loan> pageLoan = loanRepository.getAllLoanHistoryByUserAndStoreAndMonthYear(selectedUser.getId(), selectedStore.getId(), month, year, pageDetails);

        List<Loan> loanList = pageLoan.getContent();
        List<LoanDTO> loanDTOList = loanList.stream().map(loan -> modelMapper.map(loan, LoanDTO.class)).toList();

        LoanPagination loanPagination = new LoanPagination();
        loanPagination.setContent(loanDTOList);
        loanPagination.setPageNumber(pageLoan.getNumber());
        loanPagination.setPageSize(pageLoan.getSize());
        loanPagination.setTotalElements(pageLoan.getTotalElements());
        loanPagination.setTotalPages(pageLoan.getTotalPages());
        loanPagination.setLastPage(pageLoan.isLast());
        return loanPagination;
    }

    @Transactional
    @Override
    public void addLoan(Long userId, String currentLoggedIn, int newLoanAmount) {
        User selectedUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id: " + userId + " is not found!"));

        // CHANGE
        User currentUser = userRepository.findByUsernameAndIsActiveTrue(currentLoggedIn)
                .orElseThrow(() -> new ResourceNotFoundException("User with username: " + currentLoggedIn + " is not found!"));

        Salary latestSalary = salaryRepository.findLatestActiveSalaryByUserAndOptionalDate(selectedUser.getId(), null)
                .orElseThrow(() -> new ResourceNotFoundException("Salary is not found!"));

        LocalDate currentDate = LocalDate.now();
        Loan existingLoan = loanRepository.getLoanByUserAndStoreAndDate(selectedUser.getId(), selectedUser.getStore().getId(), currentDate).orElse(null);
        if (existingLoan != null) {
            throw new InternalServerErrorException("Loan action can be perform once a day!");
        }

        int validAttendancesCount = attendanceRepository.countOnTimeAndLateByYearAndMonth(selectedUser.getId(), currentDate.getMonthValue(), currentDate.getYear()).orElse(0);

        int totalOvertimePay = attendanceRepository.getTotalOvertimePayByYearAndMonth(selectedUser.getId(), currentDate.getMonthValue(), currentDate.getYear()).orElse(0);
        int totalLoan = loanRepository.getTotalLoanByUserAndStoreAndMonthYear(selectedUser.getId(), selectedUser.getStore().getId(), currentDate.getMonthValue(), currentDate.getYear()).orElse(0);

        int totalDeduction = attendanceRepository.getTotalDeductionByYearAndMonth(selectedUser.getId(), currentDate.getMonthValue(), currentDate.getYear()).orElse(0);

        int baseSalary = validAttendancesCount * latestSalary.getAmount();
        int totalSalary = baseSalary - (totalLoan + totalDeduction) + totalOvertimePay;

        if (newLoanAmount > totalSalary) {
            throw new BadRequestException("Can't perform action, your current salary: Rp. " + totalSalary);
        }

        try {
            Loan newLoan = new Loan();
            newLoan.setAmount(newLoanAmount);
            newLoan.setUser(selectedUser);
            newLoan.setStore(selectedUser.getStore());
            loanRepository.save(newLoan);

            if (currentUser.getRole().getName() == RoleEnum.ROLE_ADMIN) {
                String activityDescription = currentUser.getUsername() + " is add a new loan for " + selectedUser.getUsername() + " with amount of Rp. " + newLoanAmount;
                activityLogService.addActivityLog(currentUser, "ADD", "Add new loan", "Loan", activityDescription);
            }

        } catch (Exception e) {
            throw new InternalServerErrorException("Failed to add new loan!");
        }
    }

    @Transactional
    @Override
    public void updateLoan(Long loanId, String currentLoggedIn, int newLoanAmount) {
        Loan selectedLoan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan with id: " + loanId + " is not found!"));

        // CHANGE
        User currentUser = userRepository.findByUsernameAndIsActiveTrue(currentLoggedIn)
                .orElseThrow(() -> new ResourceNotFoundException("User with username: " + currentLoggedIn + " is not found!"));

        Salary latestSalary = salaryRepository.findLatestActiveSalaryByUserAndOptionalDate(selectedLoan.getUser().getId(), null)
                .orElseThrow(() -> new ResourceNotFoundException("Salary is not found!"));

        LocalDate currentDate = LocalDate.now();

        int validAttendancesCount = attendanceRepository.countOnTimeAndLateByYearAndMonth(selectedLoan.getUser().getId(), currentDate.getMonthValue(), currentDate.getYear()).orElse(0);

        int totalLoan = loanRepository.getTotalLoanByUserAndStoreAndMonthYear(selectedLoan.getUser().getId(), selectedLoan.getUser().getStore().getId(), currentDate.getMonthValue(), currentDate.getYear()).orElse(0);

        int totalDeduction = attendanceRepository.getTotalDeductionByYearAndMonth(selectedLoan.getUser().getId(), currentDate.getMonthValue(), currentDate.getYear()).orElse(0);

        int baseSalary = validAttendancesCount * latestSalary.getAmount();
        int totalSalary = baseSalary - (totalLoan - selectedLoan.getAmount() + totalDeduction);

        if (newLoanAmount > totalSalary) {
            throw new BadRequestException("Can't perform action, your current salary: Rp. " + totalSalary);
        }

        try {
            selectedLoan.setAmount(newLoanAmount);
            selectedLoan.setUpdatedDate(LocalDateTime.now());
            loanRepository.save(selectedLoan);

            if (currentUser.getRole().getName() == RoleEnum.ROLE_ADMIN) {
                String activityDescription = currentUser.getUsername() + " changes " + selectedLoan.getUser().getUsername() + " loan's amount, created on " + currentDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")) + ", to Rp. " + newLoanAmount;
                activityLogService.addActivityLog(currentUser, "UPDATE", "Update Loan", "Loan", activityDescription);
            }

        } catch (Exception e) {
            throw new InternalServerErrorException("Failed to update loan!");
        }
    }
}
