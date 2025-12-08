package com.dev.attendo.controller;

import com.dev.attendo.dtos.employeeLeave.LeaveDTO;
import com.dev.attendo.dtos.employeeLeave.LeavePagination;
import com.dev.attendo.security.response.MessageResponse;
import com.dev.attendo.service.LeaveApplicationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/leave")
public class LeaveController {

    @Autowired
    LeaveApplicationService leaveApplicationService;

    @GetMapping("/{leaveId}")
    public ResponseEntity<?> getLeaveApplication(@PathVariable Long leaveId) {
        return ResponseEntity.ok(leaveApplicationService.getLeaveApplication(leaveId));
    }

    @GetMapping
    public ResponseEntity<?> getAllLeaveApplication(
            @RequestParam(defaultValue = "", required = false) String keyword,
            @RequestParam(name = "currentLoggedIn") String currentUser,
            @RequestParam(name = "store") Long storeId,
            @RequestParam(defaultValue = "PENDING", required = false) String status,
            @RequestParam(defaultValue = "All", required = false) String type,
            @RequestParam(name = "startDate", required = false) LocalDate selectedStartDate,
            @RequestParam(name = "endDate", required = false) LocalDate selectedEndDate,
            @RequestParam(defaultValue = "0", required = false) Integer pageNumber,
            @RequestParam(defaultValue = "5", required = false) Integer pageSize,
            @RequestParam(defaultValue = "createdDate", required = false) String sortBy,
            @RequestParam(defaultValue = "desc", required = false) String sortOrder
    ) {
        LeavePagination leaveTicketPagination = leaveApplicationService.getAllLeaveApplication(currentUser, keyword, status, type, storeId, selectedStartDate, selectedEndDate, pageNumber, pageSize, sortBy, sortOrder);
        return ResponseEntity.ok(leaveTicketPagination);
    }

    @GetMapping("/requested/{userId}")
    public ResponseEntity<?> getAllRequestedLeaveApplication(
            @PathVariable Long userId,
            @RequestParam(name = "store") Long storeId,
            @RequestParam(defaultValue = "PENDING", required = false) String status,
            @RequestParam(defaultValue = "All", required = false) String type,
            @RequestParam(name = "startDate", required = false) LocalDate selectedStartDate,
            @RequestParam(name = "endDate", required = false) LocalDate selectedEndDate,
            @RequestParam(defaultValue = "0", required = false) Integer pageNumber,
            @RequestParam(defaultValue = "5", required = false) Integer pageSize,
            @RequestParam(defaultValue = "createdDate", required = false) String sortBy,
            @RequestParam(defaultValue = "desc", required = false) String sortOrder
    ) {
        LeavePagination requestedLeavePagination = leaveApplicationService.getAllRequestedLeaveApplication(userId, status, type, storeId, selectedStartDate, selectedEndDate, pageNumber, pageSize, sortBy, sortOrder);
        return ResponseEntity.ok(requestedLeavePagination);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @PostMapping("/{userId}")
    public ResponseEntity<?> addNewLeaveApplication(@PathVariable Long userId, @Valid @RequestBody LeaveDTO leaveDTO) {
        leaveApplicationService.addLeaveApplication(userId, leaveDTO);
        return ResponseEntity.ok(new MessageResponse(true, "Data pengajuan perizinan baru berhasil ditambahkan!"));
    }

    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @PutMapping("/{leaveId}")
    public ResponseEntity<?> updateLeaveApplication(
            @PathVariable Long leaveId,
            @RequestParam(name = "approver") String approverName,
            @RequestParam(name = "status") String approvalStatus
    ) {
        leaveApplicationService.updateLeaveApplication(leaveId, approverName, approvalStatus);
        return ResponseEntity.ok(new MessageResponse(true, "Data pengajuan perizinan berhasil diubah!"));
    }

    @DeleteMapping("/{leaveId}")
    public ResponseEntity<?> deleteLeaveApplication(@PathVariable Long leaveId) {
        leaveApplicationService.deleteLeaveApplication(leaveId);
        return ResponseEntity.ok(new MessageResponse(true, "Data pengajuan perizinan berhasil dihapus!"));
    }
}
