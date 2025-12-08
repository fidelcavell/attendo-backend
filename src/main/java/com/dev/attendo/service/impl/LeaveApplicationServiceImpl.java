package com.dev.attendo.service.impl;

import com.dev.attendo.dtos.employeeLeave.LeaveDTO;
import com.dev.attendo.dtos.employeeLeave.LeavePagination;
import com.dev.attendo.exception.BadRequestException;
import com.dev.attendo.exception.InternalServerErrorException;
import com.dev.attendo.exception.ResourceNotFoundException;
import com.dev.attendo.model.Attendance;
import com.dev.attendo.model.LeaveApplication;
import com.dev.attendo.model.User;
import com.dev.attendo.repository.AttendanceRepository;
import com.dev.attendo.repository.LeaveApplicationRepository;
import com.dev.attendo.repository.UserRepository;
import com.dev.attendo.service.ActivityLogService;
import com.dev.attendo.service.LeaveApplicationService;
import com.dev.attendo.utils.enums.*;
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
import java.util.Objects;

@Service
public class LeaveApplicationServiceImpl implements LeaveApplicationService {

    @Autowired
    LeaveApplicationRepository leaveApplicationRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    AttendanceRepository attendanceRepository;

    @Autowired
    ActivityLogService activityLogService;

    @Autowired
    ModelMapper modelMapper;

    @Override
    public LeavePagination getAllLeaveApplication(String currentUser, String keyword, String status, String type, Long storeId, LocalDate selectedStartDate, LocalDate selectedEndDate, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        User selectedUser = userRepository.findByUsernameAndIsActiveTrue(currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("User dengan username: " + currentUser + " tidak ditemukan!"));

        LeaveTypeEnum typeEnum = Objects.equals(type, "All") ? null : LeaveTypeEnum.valueOf(type);

        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Page<LeaveApplication> pageLeaveTicket = leaveApplicationRepository.findByCurrentUserRoleAndStoreAndNameAndStatusAndType(selectedUser.getRole().getName().name(), storeId, '%' + keyword + '%', ApprovalStatusEnum.valueOf(status), typeEnum, selectedStartDate, selectedEndDate, pageDetails);

        List<LeaveApplication> leaveTicketList = pageLeaveTicket.getContent();
        List<LeaveDTO> leaveTicketDTOList = leaveTicketList.stream().map(leaveTicket -> {
            LeaveDTO mappingResult = modelMapper.map(leaveTicket, LeaveDTO.class);
            mappingResult.setIssuedBy(leaveTicket.getUser().getUsername());
            mappingResult.setApprovedBy(leaveTicket.getApprover() == null ? "-" : leaveTicket.getApprover().getUsername());
            return mappingResult;
        }).toList();

        LeavePagination leaveTicketPagination = new LeavePagination();
        leaveTicketPagination.setContent(leaveTicketDTOList);
        leaveTicketPagination.setPageNumber(pageLeaveTicket.getNumber());
        leaveTicketPagination.setPageSize(pageLeaveTicket.getSize());
        leaveTicketPagination.setTotalElements(pageLeaveTicket.getTotalElements());
        leaveTicketPagination.setTotalPages(pageLeaveTicket.getTotalPages());
        leaveTicketPagination.setLastPage(pageLeaveTicket.isLast());
        return leaveTicketPagination;
    }

    @Override
    public LeavePagination getAllRequestedLeaveApplication(Long userId, String status, String type, Long storeId, LocalDate selectedStartDate, LocalDate selectedEndDate, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        User selectedUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User dengan id: " + userId + " tidak ditemukan!"));

        LeaveTypeEnum typeEnum = Objects.equals(type, "All") ? null : LeaveTypeEnum.valueOf(type);

        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);

        Page<LeaveApplication> pageLeaveApplication = leaveApplicationRepository.findByUserIdAndStoreAndNameAndStatusAndType(selectedUser.getId(), storeId, selectedStartDate, selectedEndDate, ApprovalStatusEnum.valueOf(status), typeEnum, pageDetails);
        List<LeaveApplication> leaveApplicationList = pageLeaveApplication.getContent();
        List<LeaveDTO> leaveTicketDTOList = leaveApplicationList.stream().map(leaveApplication -> {
            LeaveDTO mappingResult = modelMapper.map(leaveApplication, LeaveDTO.class);
            mappingResult.setIssuedBy(leaveApplication.getUser().getUsername());
            mappingResult.setApprovedBy(leaveApplication.getApprover() == null ? "-" : leaveApplication.getApprover().getUsername());
            return mappingResult;
        }).toList();

        LeavePagination leaveApplicationPagination = new LeavePagination();
        leaveApplicationPagination.setContent(leaveTicketDTOList);
        leaveApplicationPagination.setPageNumber(pageLeaveApplication.getNumber());
        leaveApplicationPagination.setPageSize(pageLeaveApplication.getSize());
        leaveApplicationPagination.setTotalElements(pageLeaveApplication.getTotalElements());
        leaveApplicationPagination.setTotalPages(pageLeaveApplication.getTotalPages());
        leaveApplicationPagination.setLastPage(pageLeaveApplication.isLast());
        return leaveApplicationPagination;
    }

    @Override
    public LeaveDTO getLeaveApplication(Long leaveTicketId) {
        LeaveApplication selectedLeaveApplication = leaveApplicationRepository.findById(leaveTicketId)
                .orElseThrow(() -> new ResourceNotFoundException("Pengajuan perizinan dengan id: " + leaveTicketId + " tidak ditemukan!"));

        LeaveDTO leaveApplicationDTO = modelMapper.map(selectedLeaveApplication, LeaveDTO.class);
        leaveApplicationDTO.setIssuedBy(selectedLeaveApplication.getUser().getUsername());
        leaveApplicationDTO.setApprovedBy(selectedLeaveApplication.getApprover() == null ? "-" : selectedLeaveApplication.getApprover().getUsername());
        return leaveApplicationDTO;
    }

    @Transactional
    @Override
    public void addLeaveApplication(Long employeeId, LeaveDTO leaveDTO) {
        User applicant = userRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Data karyawan dengan id: " + employeeId + " tidak ditemukan!"));

        try {
            LeaveApplication leaveApplication = new LeaveApplication();
            leaveApplication.setType(leaveDTO.getType());
            leaveApplication.setStartDate(leaveDTO.getStartDate());
            leaveApplication.setEndDate(leaveDTO.getEndDate());
            leaveApplication.setDescription(leaveDTO.getDescription());
            leaveApplication.setStatus(ApprovalStatusEnum.PENDING);
            leaveApplication.setUser(applicant);
            leaveApplication.setStore(applicant.getStore());
            leaveApplicationRepository.save(leaveApplication);

        } catch (Exception e) {
            throw new InternalServerErrorException("Gagal menambahkan pengajuan perizinan baru!");
        }
    }

    @Transactional
    @Override
    public void updateLeaveApplication(Long leaveId, String approverName, String approvalStatus) {
        LeaveApplication selectedLeaveApplication = leaveApplicationRepository.findById(leaveId)
                .orElseThrow(() -> new ResourceNotFoundException("Pengajuan perizinan dengan id: " + leaveId + " tidak ditemukan!"));
        User approver = userRepository.findByUsernameAndIsActiveTrue(approverName)
                .orElseThrow(() -> new ResourceNotFoundException("Approver dengan nama: " + approverName + " tidak ditemukan!"));
        User applicant = selectedLeaveApplication.getUser();

        try {
            // Leave Application will get updated based on approval type that been set.
            selectedLeaveApplication.setStatus(ApprovalStatusEnum.valueOf(approvalStatus));
            selectedLeaveApplication.setApprover(approver);
            selectedLeaveApplication.setUpdatedDate(LocalDateTime.now());
            leaveApplicationRepository.save(selectedLeaveApplication);

            // If Leave Ticket get approved then new attendance with ON_LEAVE type is being created.
            if (ApprovalStatusEnum.valueOf(approvalStatus) == ApprovalStatusEnum.APPROVED) {
                for (LocalDate date = selectedLeaveApplication.getStartDate(); !date.isAfter(selectedLeaveApplication.getEndDate()); date = date.plusDays(1)) {
                    Attendance leaveAttendance = new Attendance();
                    leaveAttendance.setType(AttendanceTypeEnum.LEAVE);
                    leaveAttendance.setStatus(AttendanceStatusEnum.LEAVE);
                    leaveAttendance.setClockIn(date.atTime(applicant.getProfile().getSchedule().getStartTime()));
                    leaveAttendance.setClockOut(date.atTime(applicant.getProfile().getSchedule().getEndTime()));
                    leaveAttendance.setDescription(selectedLeaveApplication.getDescription());
                    leaveAttendance.setLeaveApplication(selectedLeaveApplication);
                    leaveAttendance.setUser(applicant);
                    leaveAttendance.setStore(applicant.getStore());
                    attendanceRepository.save(leaveAttendance);
                }
            }

            // Write Audit Log if approver has role as ADMIN
            if (approver.getRole().getName() == RoleEnum.ROLE_ADMIN) {
                String activityDescription = approver.getUsername() + " melakukan " + approvalStatus + " pada pengajuan perizinan dengan tanggal " + selectedLeaveApplication.getStartDate().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")) + " - " + selectedLeaveApplication.getEndDate().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")) + " yang dilakukan oleh " + applicant.getUsername();
                activityLogService.addActivityLog(approver, "UPDATE", "Update Leave Application", "LeaveApplication", activityDescription);
            }

        } catch (Exception e) {
            throw new InternalServerErrorException("Gagal mengubah pengajuan perizinan!");
        }
    }

    @Transactional
    @Override
    public void deleteLeaveApplication(Long leaveId) {
        LeaveApplication selectedLeaveApplication = leaveApplicationRepository.findById(leaveId)
                .orElseThrow(() -> new ResourceNotFoundException("Pengajuan perizinan dengan id: " + leaveId + " tidak ditemukan!"));

        if (selectedLeaveApplication.getStatus() != ApprovalStatusEnum.PENDING) {
            throw new BadRequestException("Tidak bisa menghapus pengajuan dengan status APPROVED atau REJECTED!");
        }

        try {
            leaveApplicationRepository.delete(selectedLeaveApplication);

        } catch (Exception e) {
            throw new InternalServerErrorException("Gagal menghapus pengajuan perizinan!");
        }
    }
}
