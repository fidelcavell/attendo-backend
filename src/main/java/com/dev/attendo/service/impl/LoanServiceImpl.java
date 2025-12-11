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
                .orElseThrow(() -> new ResourceNotFoundException("User dengan id: " + userId + " tidak ditemukan!"));

        Store selectedStore = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Data toko dengan id: " + storeId + " tidak ditemukan!"));

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
                .orElseThrow(() -> new ResourceNotFoundException("User dengan id: " + userId + " tidak ditemukan!"));

        User currentUser = userRepository.findByUsernameAndIsActiveTrue(currentLoggedIn)
                .orElseThrow(() -> new ResourceNotFoundException("User dengan username: " + currentLoggedIn + " tidak ditemukan!"));

        Salary latestSalary = salaryRepository.findLatestActiveSalaryByUserAndOptionalDate(selectedUser.getId(), null)
                .orElseThrow(() -> new ResourceNotFoundException("Data gaji tidak ditemukan!"));

        LocalDate currentDate = LocalDate.now();
        Loan existingLoan = loanRepository.getLoanByUserAndStoreAndDate(selectedUser.getId(), selectedUser.getStore().getId(), currentDate).orElse(null);
        if (existingLoan != null) {
            throw new InternalServerErrorException("Aksi penambahan data peminjaman hanya bisa dilakukan sehari sekali!");
        }

        int validAttendancesCount = attendanceRepository.countOnTimeAndLateByYearAndMonth(selectedUser.getId(), currentDate.getMonthValue(), currentDate.getYear()).orElse(0);

        int totalOvertimePay = attendanceRepository.getTotalOvertimePayByYearAndMonth(selectedUser.getId(), currentDate.getMonthValue(), currentDate.getYear()).orElse(0);
        int totalLoan = loanRepository.getTotalLoanByUserAndStoreAndMonthYear(selectedUser.getId(), selectedUser.getStore().getId(), currentDate.getMonthValue(), currentDate.getYear()).orElse(0);

        int totalDeduction = attendanceRepository.getTotalDeductionByYearAndMonth(selectedUser.getId(), currentDate.getMonthValue(), currentDate.getYear()).orElse(0);

        int baseSalary = validAttendancesCount * latestSalary.getAmount();
        int totalSalary = baseSalary - (totalLoan + totalDeduction) + totalOvertimePay;

        if (newLoanAmount > totalSalary) {
            throw new BadRequestException("Aksi tidak bisa dilakukan, gaji yang telah dimiliki: Rp. " + totalSalary);
        }

        try {
            Loan newLoan = new Loan();
            newLoan.setAmount(newLoanAmount);
            newLoan.setUser(selectedUser);
            newLoan.setStore(selectedUser.getStore());
            loanRepository.save(newLoan);

            if (currentUser.getRole().getName() == RoleEnum.ROLE_ADMIN) {
                String activityDescription = currentUser.getUsername() + " menambahkan data peminjaman uang baru pada " + selectedUser.getUsername() + " dengan jumlah sebesar Rp. " + newLoanAmount;
                activityLogService.addActivityLog(currentUser, "ADD", "Tambah Peminjaman Uang Baru", "Peminjaman Uang", activityDescription);
            }

        } catch (Exception e) {
            throw new InternalServerErrorException("Gagal menambahkan data peminjaman uang baru!");
        }
    }

    @Transactional
    @Override
    public void updateLoan(Long loanId, String currentLoggedIn, int newLoanAmount) {
        Loan selectedLoan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Data peminjaman uang dengan id: " + loanId + " tidak ditemukan!"));

        User currentUser = userRepository.findByUsernameAndIsActiveTrue(currentLoggedIn)
                .orElseThrow(() -> new ResourceNotFoundException("User dengan username: " + currentLoggedIn + " tidak ditemukan!"));

        Salary latestSalary = salaryRepository.findLatestActiveSalaryByUserAndOptionalDate(selectedLoan.getUser().getId(), null)
                .orElseThrow(() -> new ResourceNotFoundException("Data gaji tidak ditemukan!"));

        LocalDate currentDate = LocalDate.now();

        int validAttendancesCount = attendanceRepository.countOnTimeAndLateByYearAndMonth(selectedLoan.getUser().getId(), currentDate.getMonthValue(), currentDate.getYear()).orElse(0);

        int totalLoan = loanRepository.getTotalLoanByUserAndStoreAndMonthYear(selectedLoan.getUser().getId(), selectedLoan.getUser().getStore().getId(), currentDate.getMonthValue(), currentDate.getYear()).orElse(0);

        int totalDeduction = attendanceRepository.getTotalDeductionByYearAndMonth(selectedLoan.getUser().getId(), currentDate.getMonthValue(), currentDate.getYear()).orElse(0);

        int baseSalary = validAttendancesCount * latestSalary.getAmount();
        int totalSalary = baseSalary - (totalLoan - selectedLoan.getAmount() + totalDeduction);

        if (newLoanAmount > totalSalary) {
            throw new BadRequestException("Aksi tidak bisa dilakukan, gaji yang telah dimiliki: Rp. " + totalSalary);
        }

        try {
            selectedLoan.setAmount(newLoanAmount);
            selectedLoan.setUpdatedDate(LocalDateTime.now());
            loanRepository.save(selectedLoan);

            if (currentUser.getRole().getName() == RoleEnum.ROLE_ADMIN) {
                String activityDescription = currentUser.getUsername() + " mengubah data peminjaman uang milik " + selectedLoan.getUser().getUsername() +  " pada " + currentDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")) + " dengan jumlah sebesar Rp. " + newLoanAmount;
                activityLogService.addActivityLog(currentUser, "UPDATE", "Update Peminjaman Uang", "Peminjaman Uang", activityDescription);
            }

        } catch (Exception e) {
            throw new InternalServerErrorException("Gagal mengubah data peminjaman uang!");
        }
    }
}
