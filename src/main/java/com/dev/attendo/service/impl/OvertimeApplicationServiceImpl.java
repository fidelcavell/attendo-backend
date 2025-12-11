package com.dev.attendo.service.impl;

import com.dev.attendo.dtos.overtime.OvertimeDTO;
import com.dev.attendo.dtos.overtime.OvertimePagination;
import com.dev.attendo.exception.BadRequestException;
import com.dev.attendo.exception.InternalServerErrorException;
import com.dev.attendo.exception.ResourceNotFoundException;
import com.dev.attendo.model.*;
import com.dev.attendo.repository.*;
import com.dev.attendo.service.ActivityLogService;
import com.dev.attendo.service.OvertimeApplicationService;
import com.dev.attendo.utils.enums.ApprovalStatusEnum;
import com.dev.attendo.utils.enums.AttendanceStatusEnum;
import com.dev.attendo.utils.enums.AttendanceTypeEnum;
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
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class OvertimeApplicationServiceImpl implements OvertimeApplicationService {

    @Autowired
    OvertimeApplicationRepository overtimeRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ScheduleRepository scheduleRepository;

    @Autowired
    SalaryRepository salaryRepository;

    @Autowired
    AttendanceRepository attendanceRepository;

    @Autowired
    ActivityLogService activityLogService;

    @Autowired
    ModelMapper modelMapper;

    @Override
    public OvertimeDTO getOvertimeApplicationById(Long overtimeId) {
        OvertimeApplication selectedOvertimeApplication = overtimeRepository.findById(overtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("Pengajuan lembur dengan id: " + overtimeId + " tidak ditemukan!"));
        OvertimeDTO overtimeApplicationDTO = modelMapper.map(selectedOvertimeApplication, OvertimeDTO.class);
        overtimeApplicationDTO.setAssignedTime(selectedOvertimeApplication.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")) + " WIB - " + selectedOvertimeApplication.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm")) + " WIB");
        overtimeApplicationDTO.setIssuedBy(selectedOvertimeApplication.getUser().getUsername());
        overtimeApplicationDTO.setApprovedBy(selectedOvertimeApplication.getApprover() == null ? "-" : selectedOvertimeApplication.getApprover().getUsername());
        return overtimeApplicationDTO;
    }

    @Override
    public OvertimePagination getAllOvertimeApplication(Long storeId, String currentUser, String keyword, String status, LocalDate selectedStartDate, LocalDate selectedEndDate, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        User selectedUser = userRepository.findByUsernameAndIsActiveTrue(currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("User dengan username: " + currentUser + " tidak ditemukan!"));

        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Page<OvertimeApplication> pageOvertimeApplication = overtimeRepository.findByCurrentUserRoleAndStoreAndNameAndStatus(storeId, selectedUser.getRole().getName().name(), '%' + keyword + '%', ApprovalStatusEnum.valueOf(status), selectedStartDate, selectedEndDate, pageDetails);

        List<OvertimeApplication> overtimeApplicationList = pageOvertimeApplication.getContent();
        List<OvertimeDTO> overtimeDTOList = overtimeApplicationList.stream().map(overtimeApplication -> {
            OvertimeDTO mappingResult = modelMapper.map(overtimeApplication, OvertimeDTO.class);
            mappingResult.setAssignedTime(overtimeApplication.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")) + " WIB - " + overtimeApplication.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm")) + " WIB");
            mappingResult.setIssuedBy(overtimeApplication.getUser().getUsername());
            mappingResult.setApprovedBy(overtimeApplication.getApprover() == null ? "-" : overtimeApplication.getApprover().getUsername());
            return mappingResult;
        }).toList();

        OvertimePagination overtimeApplicationPagination = new OvertimePagination();
        overtimeApplicationPagination.setContent(overtimeDTOList);
        overtimeApplicationPagination.setPageNumber(pageOvertimeApplication.getNumber());
        overtimeApplicationPagination.setPageSize(pageOvertimeApplication.getSize());
        overtimeApplicationPagination.setTotalElements(pageOvertimeApplication.getTotalElements());
        overtimeApplicationPagination.setTotalPages(pageOvertimeApplication.getTotalPages());
        overtimeApplicationPagination.setLastPage(pageOvertimeApplication.isLast());
        return overtimeApplicationPagination;
    }

    @Override
    public OvertimePagination getAllRequestedOvertimeApplication(Long userId, Long storeId, String status, LocalDate selectedStartDate, LocalDate selectedEndDate, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        User selectedUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User dengan id: " + userId + " tidak ditemukan!"));

        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Page<OvertimeApplication> pageOvertimeApplication = overtimeRepository.findByUserIdAndStoreAndNameAndStatus(selectedUser.getId(), selectedUser.getStore().getId(), selectedStartDate, selectedEndDate, ApprovalStatusEnum.valueOf(status), pageDetails);

        List<OvertimeApplication> overtimeApplicationList = pageOvertimeApplication.getContent();

        List<OvertimeDTO> overtimeDTOList = overtimeApplicationList.stream().map(overtimeApplication -> {
            OvertimeDTO mappingResult = modelMapper.map(overtimeApplication, OvertimeDTO.class);
            mappingResult.setAssignedTime(overtimeApplication.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")) + " WIB - " + overtimeApplication.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm")) + " WIB");
            mappingResult.setIssuedBy(overtimeApplication.getUser().getUsername());
            mappingResult.setApprovedBy(overtimeApplication.getApprover() == null ? "-" : overtimeApplication.getApprover().getUsername());
            return mappingResult;
        }).toList();

        OvertimePagination overtimeApplicationPagination = new OvertimePagination();
        overtimeApplicationPagination.setContent(overtimeDTOList);
        overtimeApplicationPagination.setPageNumber(pageOvertimeApplication.getNumber());
        overtimeApplicationPagination.setPageSize(pageOvertimeApplication.getSize());
        overtimeApplicationPagination.setTotalElements(pageOvertimeApplication.getTotalElements());
        overtimeApplicationPagination.setTotalPages(pageOvertimeApplication.getTotalPages());
        overtimeApplicationPagination.setLastPage(pageOvertimeApplication.isLast());
        return overtimeApplicationPagination;
    }

    @Transactional
    @Override
    public void addNewOvertimeApplication(Long userId, Long scheduleId, OvertimeDTO overtimeDTO) {
        User selectedUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User dengan id: " + userId + " tidak ditemukan!"));
        Schedule selectedSchedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Data jadwal kerja dengan id: " + scheduleId + " tidak ditemukan!"));

        Salary selectedUserSalary = salaryRepository.findLatestActiveSalaryByUserAndOptionalDate(selectedUser.getId(), null).orElse(null);
        int latestSalaryAmount = selectedUserSalary != null ? selectedUserSalary.getAmount() : 0;
        int overtimePay = (int) (selectedUser.getStore().getMultiplierOvertime() * latestSalaryAmount);

        try {
            OvertimeApplication overtimeApplication = modelMapper.map(overtimeDTO, OvertimeApplication.class);
            overtimeApplication.setStatus(ApprovalStatusEnum.PENDING);
            overtimeApplication.setOvertimePay(overtimePay);
            overtimeApplication.setStartTime(selectedSchedule.getStartTime());
            overtimeApplication.setEndTime(selectedSchedule.getEndTime());
            overtimeApplication.setLateTolerance(selectedSchedule.getLateTolerance());
            overtimeApplication.setUser(selectedUser);

            overtimeRepository.save(overtimeApplication);

        } catch (Exception e) {
            throw new InternalServerErrorException("Gagal menambahkan pengajuan lembur yang baru!");
        }
    }

    @Transactional
    @Override
    public void updateOvertimeApplication(Long overtimeId, String approverName, String approvalStatus) {
        OvertimeApplication selectedOvertimeApplication = overtimeRepository.findById(overtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("Pengajuan lembur dengan id: " + overtimeId + " tidak ditemukan!"));
        User approver = userRepository.findByUsernameAndIsActiveTrue(approverName)
                .orElseThrow(() -> new ResourceNotFoundException("User dengan username: " + approverName + " tidak ditemukan!"));
        User applicant = selectedOvertimeApplication.getUser();

        Salary currentSalary = salaryRepository.findLatestActiveSalaryByUserAndOptionalDate(applicant.getId(), null)
                .orElseThrow(() -> new ResourceNotFoundException("Data gaji belum didefinisikan!"));

        try {
            if (ApprovalStatusEnum.valueOf(approvalStatus) == ApprovalStatusEnum.APPROVED) {
                double overtimeSalary = currentSalary.getAmount() * applicant.getStore().getMultiplierOvertime();
                selectedOvertimeApplication.setOvertimePay((int) overtimeSalary);
            }
            selectedOvertimeApplication.setStatus(ApprovalStatusEnum.valueOf(approvalStatus));
            selectedOvertimeApplication.setApprover(approver);
            overtimeRepository.save(selectedOvertimeApplication);

            // Create new attendance's data with overtime type -> now user can do clock in/out and break in/out on that overtime schedule
            Attendance overtimeAttendance = new Attendance();
            overtimeAttendance.setType(AttendanceTypeEnum.OVERTIME);
            overtimeAttendance.setStatus(AttendanceStatusEnum.ABSENT);
            overtimeAttendance.setDescription("");
            overtimeAttendance.setDeductionAmount(0);
            overtimeAttendance.setLateInMinutes(0);
            overtimeAttendance.setCreatedDate(selectedOvertimeApplication.getOvertimeDate().atStartOfDay());
            overtimeAttendance.setOvertimeApplication(selectedOvertimeApplication);
            overtimeAttendance.setUser(applicant);
            overtimeAttendance.setStore(applicant.getStore());
            attendanceRepository.save(overtimeAttendance);

            if (approver.getRole().getName() == RoleEnum.ROLE_ADMIN) {
                String scheduleTime = selectedOvertimeApplication.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")) + " WIB" + " - " + selectedOvertimeApplication.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")) + " WIB";

                String activityDescription = approver.getUsername() + " melakukan " + approvalStatus + " pada pengajuan lembur dengan tanggal " + selectedOvertimeApplication.getOvertimeDate().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")) + " dengan jadwal kerja: " + scheduleTime + " yang dilakukan oleh " + applicant.getUsername();
                activityLogService.addActivityLog(approver, "UPDATE", "Update Pengajuan Lembur", "Lembur", activityDescription);
            }

        } catch (Exception e) {
            throw new InternalServerErrorException("Gagal mengubah pengajuan lembur!");
        }
    }

    @Transactional
    @Override
    public void deleteOvertimeApplication(Long overtimeId) {
        OvertimeApplication selectedOvertimeApplication = overtimeRepository.findById(overtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("Pengajuan lembur dengan id: " + overtimeId + " tidak ditemukan!"));

        if (selectedOvertimeApplication.getStatus() != ApprovalStatusEnum.PENDING) {
            throw new BadRequestException("Tidak bisa menghapus pengajuan dengan status APPROVED atau REJECTED!");
        }

        try {
            overtimeRepository.delete(selectedOvertimeApplication);

        } catch (Exception e) {
            throw new InternalServerErrorException("Gagal menghapus pengajuan lembur!");
        }
    }
}
