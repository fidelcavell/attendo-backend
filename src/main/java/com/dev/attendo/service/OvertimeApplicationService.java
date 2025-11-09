package com.dev.attendo.service;

import com.dev.attendo.dtos.employeeLeave.LeavePagination;
import com.dev.attendo.dtos.overtime.OvertimeDTO;
import com.dev.attendo.dtos.overtime.OvertimePagination;
import org.hibernate.sql.ast.tree.expression.Over;

import java.time.LocalDate;

public interface OvertimeApplicationService {

    OvertimeDTO getOvertimeApplicationById(Long overtimeId);

    OvertimePagination getAllOvertimeApplication(Long storeId, String currentUser, String keyword, String status, LocalDate selectedStartDate, LocalDate selectedEndDate, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);

    OvertimePagination getAllRequestedOvertimeApplication(Long userId, Long storeId, String status, LocalDate selectedStartDate, LocalDate selectedEndDate, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);

    void addNewOvertimeApplication(Long userId, Long scheduleId, OvertimeDTO overtimeDTO);

    void updateOvertimeApplication(Long overtimeId, String approverName, String approvalStatus);

    void deleteOvertimeApplication(Long overtimeId);
}
