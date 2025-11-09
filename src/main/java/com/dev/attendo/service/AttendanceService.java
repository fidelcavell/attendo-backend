package com.dev.attendo.service;

import com.dev.attendo.dtos.attendance.AttendanceDTO;
import com.dev.attendo.dtos.attendance.AttendancePagination;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface AttendanceService {

    void overtimeClockIn(Long attendanceId, LocalDateTime currentDateTime, MultipartFile photo, double lat, double lng);

    void overtimeClockOut(Long attendanceId, LocalDateTime currentDateTime, MultipartFile photo, double lat, double lng);

    void dailyClockIn(String username, LocalDateTime currentDateTime, MultipartFile photo, double lat, double lng);

    void dailyClockOut(Long attendanceId, LocalDateTime currentDateTime, MultipartFile photo, double lat, double lng);

    void breakIn(Long attendanceId, LocalDateTime currentDateTime);

    void breakOut(Long attendanceId, LocalDateTime currentDateTime);

    Map<String, Object> getAttendance(Long attendanceId);

    void updateAttendance(Long attendanceId, String currentLoggedIn, String attendanceStatus, int deductionAmount, String attendanceDescription);

    void removeAttendance(Long attendanceId, String currentLoggedIn);

    List<AttendanceDTO> getAttendanceByMonthAndYear(Long userId, int month, int year);

    byte[] getAttendancePhoto(Long attendanceId, String type);

    String getInAreaStatus(Long storeId, double lat, double lng);

    AttendanceDTO getTodayOvertimeSchedule(Long userId);

    AttendanceDTO getTodayDailyAttendanceInfo(Long userId);

    AttendancePagination getTodayAttendances(Long storeId, String type, String keyword, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);
}
