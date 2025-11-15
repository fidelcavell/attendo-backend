package com.dev.attendo.controller;

import com.dev.attendo.dtos.attendance.AttendancePagination;
import com.dev.attendo.security.response.MessageResponse;
import com.dev.attendo.service.AttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/attendance")
@PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @GetMapping("/in-area-status")
    public ResponseEntity<?> getInAreaStatus(
            @RequestParam(name = "store") Long storeId,
            @RequestParam double lat,
            @RequestParam double lng
    ) {
        String areaStatus = attendanceService.getInAreaStatus(storeId, lat, lng);
        return ResponseEntity.ok(new MessageResponse(true, areaStatus));
    }

    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @GetMapping("/today-attendances/{storeId}")
    public ResponseEntity<?> getTodayAttendanceList(
            @PathVariable Long storeId,
            @RequestParam(defaultValue = "All", required = false) String type,
            @RequestParam(defaultValue = "", required = false) String keyword,
            @RequestParam(defaultValue = "0", required = false) Integer pageNumber,
            @RequestParam(defaultValue = "5", required = false) Integer pageSize,
            @RequestParam(defaultValue = "clockIn", required = false) String sortBy,
            @RequestParam(defaultValue = "desc", required = false) String sortOrder
    ) {
        AttendancePagination todayAttendancePagination = attendanceService.getTodayAttendances(storeId, type, keyword, pageNumber, pageSize, sortBy, sortOrder);
        return ResponseEntity.ok(todayAttendancePagination);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{attendanceId}")
    public ResponseEntity<?> getAttendance(@PathVariable Long attendanceId) {
        return ResponseEntity.ok(attendanceService.getAttendance(attendanceId));
    }

    @GetMapping("/daily-info/{userId}")
    public ResponseEntity<?> getTodayDailyAttendance(@PathVariable Long userId) {
        return ResponseEntity.ok(attendanceService.getTodayDailyAttendanceInfo(userId));
    }

    @GetMapping("/overtime-info/{userId}")
    public ResponseEntity<?> getTodayOvertimeSchedule(@PathVariable Long userId) {
        return ResponseEntity.ok(attendanceService.getTodayOvertimeSchedule(userId));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/photo/{attendanceId}")
    public ResponseEntity<?> getAttendancePhoto(@PathVariable Long attendanceId, @RequestParam String type) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "image/jpeg")
                .body(attendanceService.getAttendancePhoto(attendanceId, type));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/monthly/{userId}")
    public ResponseEntity<?> getAttendancesByMonthAndYear(
            @PathVariable Long userId,
            @RequestParam int month,
            @RequestParam int year
    ) {
        return ResponseEntity.ok(attendanceService.getAttendanceByMonthAndYear(userId, month, year));
    }

    @PostMapping("/clock-in/{username}")
    public ResponseEntity<?> addClockIn(
            @PathVariable String username,
            @RequestParam LocalDateTime currentDateTime,
            @RequestParam MultipartFile photo,
            @RequestParam double lat,
            @RequestParam double lng
    ) {
        attendanceService.dailyClockIn(username, currentDateTime, photo, lat, lng);
        return ResponseEntity.ok(new MessageResponse(true, "You've successfully clocked-in!"));
    }

    @PutMapping("/clock-out/{attendanceId}")
    public ResponseEntity<?> addClockOut(
            @PathVariable Long attendanceId,
            @RequestParam LocalDateTime currentDateTime,
            @RequestParam MultipartFile photo,
            @RequestParam double lat,
            @RequestParam double lng
    ) {
        attendanceService.dailyClockOut(attendanceId, currentDateTime, photo, lat, lng);
        return ResponseEntity.ok(new MessageResponse(true, "You've successfully clocked-out!"));
    }

    @PutMapping("/break-in/{attendanceId}")
    public ResponseEntity<?> addBreakIn(
            @PathVariable Long attendanceId,
            @RequestParam LocalDateTime currentDateTime
    ) {
        attendanceService.breakIn(attendanceId, currentDateTime);
        return ResponseEntity.ok(new MessageResponse(true, "You've successfully break-in!"));
    }

    @PutMapping("/break-out/{attendanceId}")
    public ResponseEntity<?> addBreakOut(
            @PathVariable Long attendanceId,
            @RequestParam LocalDateTime currentDateTime
    ) {
        attendanceService.breakOut(attendanceId, currentDateTime);
        return ResponseEntity.ok(new MessageResponse(true, "You've successfully break-out!"));
    }

    @PutMapping("/overtime-clock-in/{attendanceId}")
    public ResponseEntity<?> addOvertimeClockIn(
            @PathVariable Long attendanceId,
            @RequestParam LocalDateTime currentDateTime,
            @RequestParam MultipartFile photo,
            @RequestParam double lat,
            @RequestParam double lng
    ) {
        attendanceService.overtimeClockIn(attendanceId, currentDateTime, photo, lat, lng);
        return ResponseEntity.ok(new MessageResponse(true, "You've successfully overtime clocked-in!"));
    }

    @PutMapping("/overtime-clock-out/{attendanceId}")
    public ResponseEntity<?> addOvertimeClockOut(
            @PathVariable Long attendanceId,
            @RequestParam LocalDateTime currentDateTime,
            @RequestParam MultipartFile photo,
            @RequestParam double lat,
            @RequestParam double lng
    ) {
        attendanceService.overtimeClockOut(attendanceId, currentDateTime, photo, lat, lng);
        return ResponseEntity.ok(new MessageResponse(true, "You've successfully overtime clocked-out!"));
    }

    @PutMapping("/overtime-break-in/{attendanceId}")
    public ResponseEntity<?> addOvertimeBreakIn(
            @PathVariable Long attendanceId,
            @RequestParam LocalDateTime currentDateTime
    ) {
        attendanceService.breakIn(attendanceId, currentDateTime);
        return ResponseEntity.ok(new MessageResponse(true, "You've successfully overtime break-in!"));
    }

    @PutMapping("/overtime-break-out/{attendanceId}")
    public ResponseEntity<?> addOvertimeBreakOut(
            @PathVariable Long attendanceId,
            @RequestParam LocalDateTime currentDateTime
    ) {
        attendanceService.breakOut(attendanceId, currentDateTime);
        return ResponseEntity.ok(new MessageResponse(true, "You've successfully overtime break-out!"));
    }

    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @DeleteMapping("/{attendanceId}")
    public ResponseEntity<?> removeAttendance(@PathVariable Long attendanceId, @RequestParam String currentLoggedIn) {
        attendanceService.removeAttendance(attendanceId, currentLoggedIn);
        return ResponseEntity.ok(new MessageResponse(true, "Attendance has been removed!"));
    }

    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @PutMapping("/{attendanceId}")
    public ResponseEntity<?> updateAttendance(
            @PathVariable Long attendanceId,
            @RequestParam String currentLoggedIn,
            @RequestParam String attendanceStatus,
            @RequestParam int deductionAmount,
            @RequestParam String attendanceDescription
    ) {
        attendanceService.updateAttendance(attendanceId, currentLoggedIn, attendanceStatus, deductionAmount, attendanceDescription);
        return ResponseEntity.ok(new MessageResponse(true, "Attendance has been updated!"));
    }
}
