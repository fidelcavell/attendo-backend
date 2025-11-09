package com.dev.attendo.service;

import com.dev.attendo.dtos.employeeLeave.LeaveDTO;
import com.dev.attendo.dtos.employeeLeave.LeavePagination;

import java.time.LocalDate;

public interface LeaveApplicationService {

    LeaveDTO getLeaveApplication(Long leaveId);

    LeavePagination getAllLeaveApplication(String currentUser, String keyword, String status, String type, Long storeId, LocalDate selectedStartDate, LocalDate selectedEndDate, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);

    LeavePagination getAllRequestedLeaveApplication(Long userId, String status, String type, Long storeId, LocalDate selectedStartDate, LocalDate selectedEndDate, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);

    void addLeaveApplication(Long employeeId, LeaveDTO leaveDTO);

    void updateLeaveApplication(Long leaveId, String approvalName, String approvalStatus);

    void deleteLeaveApplication(Long leaveId);
}
