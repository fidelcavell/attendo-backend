package com.dev.attendo.controller;

import com.dev.attendo.dtos.overtime.OvertimeDTO;
import com.dev.attendo.dtos.overtime.OvertimePagination;
import com.dev.attendo.security.response.MessageResponse;
import com.dev.attendo.service.OvertimeApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/overtime")
public class OvertimeController {

    @Autowired
    OvertimeApplicationService overtimeService;

    @GetMapping("/{overtimeId}")
    public ResponseEntity<?> getOvertimeApplication(@PathVariable Long overtimeId) {
        return ResponseEntity.ok(overtimeService.getOvertimeApplicationById(overtimeId));
    }

    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @GetMapping
    public ResponseEntity<?> getAllOvertimeApplication(
            @RequestParam(defaultValue = "", required = false) String keyword,
            @RequestParam(name = "currentLoggedIn") String currentUser,
            @RequestParam(name = "store") Long storeId,
            @RequestParam(defaultValue = "PENDING", required = false) String status,
            @RequestParam(name = "startDate", required = false) LocalDate selectedStartDate,
            @RequestParam(name = "endDate", required = false) LocalDate selectedEndDate,
            @RequestParam(defaultValue = "0", required = false) Integer pageNumber,
            @RequestParam(defaultValue = "5", required = false) Integer pageSize,
            @RequestParam(defaultValue = "createdDate", required = false) String sortBy,
            @RequestParam(defaultValue = "desc", required = false) String sortOrder
    ) {
        OvertimePagination overtimeTicketPagination = overtimeService.getAllOvertimeApplication(storeId, currentUser, keyword, status, selectedStartDate, selectedEndDate, pageNumber, pageSize, sortBy, sortOrder);
        return ResponseEntity.ok(overtimeTicketPagination);
    }

    @GetMapping("/requested/{userId}")
    public ResponseEntity<?> getAllRequestedOvertimeApplication(
            @PathVariable Long userId,
            @RequestParam(name = "store") Long storeId,
            @RequestParam(defaultValue = "PENDING", required = false) String status,
            @RequestParam(name = "startDate", required = false) LocalDate selectedStartDate,
            @RequestParam(name = "endDate", required = false) LocalDate selectedEndDate,
            @RequestParam(defaultValue = "0", required = false) Integer pageNumber,
            @RequestParam(defaultValue = "5", required = false) Integer pageSize,
            @RequestParam(defaultValue = "createdDate", required = false) String sortBy,
            @RequestParam(defaultValue = "desc", required = false) String sortOrder
    ) {
        OvertimePagination requestedOvertimePagination = overtimeService.getAllRequestedOvertimeApplication(userId, storeId, status, selectedStartDate, selectedEndDate, pageNumber, pageSize, sortBy, sortOrder);
        return ResponseEntity.ok(requestedOvertimePagination);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @PostMapping("/{userId}")
    public ResponseEntity<?> addNewOvertimeApplication(
            @PathVariable Long userId,
            @RequestParam(name = "schedule") Long scheduleId,
            @RequestBody OvertimeDTO overtimeDTO
    ) {
        overtimeService.addNewOvertimeApplication(userId, scheduleId, overtimeDTO);
        return ResponseEntity.ok(new MessageResponse(true, "New Overtime Ticket has been added!"));
    }

    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @PutMapping("/{overtimeId}")
    public ResponseEntity<?> updateOvertimeApplication(
            @PathVariable Long overtimeId,
            @RequestParam(name = "approver") String approverName,
            @RequestParam(name = "status") String approvalStatus
    ) {
        overtimeService.updateOvertimeApplication(overtimeId, approverName, approvalStatus);
        return ResponseEntity.ok(new MessageResponse(true, "Overtime Ticket has been updated!"));
    }

    @DeleteMapping("/{overtimeId}")
    public ResponseEntity<?> deleteOvertimeApplication(@PathVariable Long overtimeId) {
        overtimeService.deleteOvertimeApplication(overtimeId);
        return ResponseEntity.ok(new MessageResponse(true, "Overtime Application has been deleted!"));
    }
}
