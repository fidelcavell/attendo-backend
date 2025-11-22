package com.dev.attendo.service.impl;

import com.dev.attendo.dtos.attendance.AttendanceDTO;
import com.dev.attendo.dtos.attendance.AttendancePagination;
import com.dev.attendo.exception.BadRequestException;
import com.dev.attendo.exception.InternalServerErrorException;
import com.dev.attendo.exception.ResourceNotFoundException;
import com.dev.attendo.model.*;
import com.dev.attendo.repository.*;
import com.dev.attendo.service.ActivityLogService;
import com.dev.attendo.service.AttendanceService;
import com.dev.attendo.utils.enums.AttendanceStatusEnum;
import com.dev.attendo.utils.enums.AttendanceTypeEnum;
import com.dev.attendo.utils.enums.LeaveTypeEnum;
import com.dev.attendo.utils.enums.RoleEnum;
import com.dev.attendo.utils.helper.GeolocationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.modelmapper.ModelMapper;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

@Service
public class AttendanceServiceImpl implements AttendanceService {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private SalaryRepository salaryRepository;

    @Autowired
    private ActivityLogService activityLogService;

    @Autowired
    private ModelMapper modelMapper;

    @Transactional
    @Override
    public void overtimeClockIn(Long attendanceId, LocalDateTime currentDateTime, MultipartFile photo, double lat, double lng) {
        Attendance selectedAttendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Overtime Attendance with id: " + attendanceId + " is not found!"));
        Store selectedStore = selectedAttendance.getStore();

        // Calculate and compare distance between user's location with store location and store radius.
        double calculateInMeters = GeolocationUtils.calculateDistance(lat, lng, selectedStore.getLat(), selectedStore.getLng());
        if (calculateInMeters > selectedStore.getRadius()) {
            throw new BadRequestException("Clock-In failed: You are outside allowed area!");
        }

        try {
            selectedAttendance.setPhotoIn(photo.getBytes());
            selectedAttendance.setClockIn(currentDateTime);
            selectedAttendance.setUpdatedDate(LocalDateTime.now());
            attendanceRepository.save(selectedAttendance);

        } catch (Exception e) {
            throw new InternalServerErrorException("Failed to overtime clocked-in!");
        }
    }

    @Override
    public void overtimeClockOut(Long attendanceId, LocalDateTime currentDateTime, MultipartFile photo, double lat, double lng) {
        Attendance selectedAttendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance is not found!"));
        Store selectedStore = selectedAttendance.getStore();
        OvertimeApplication selectedOvertimeApplication = selectedAttendance.getOvertimeApplication();

        // Calculate and compare distance between user's location with store location and store radius.
        double calculateInMeters = GeolocationUtils.calculateDistance(lat, lng, selectedStore.getLat(), selectedStore.getLng());
        if (calculateInMeters > selectedStore.getRadius()) {
            throw new BadRequestException("Clocked-in failed: You are outside the allowed area!");
        }

        try {
            selectedAttendance.setPhotoOut(photo.getBytes());
            selectedAttendance.setClockOut(currentDateTime);
            selectedAttendance.setUpdatedDate(LocalDateTime.now());

            // Set schedule's start time plus late tolerate and calculate late duration between user's clocked-in time with schedule's time
            LocalDateTime startTimeSchedule = currentDateTime.toLocalDate().atTime(selectedOvertimeApplication.getStartTime().plusMinutes(selectedOvertimeApplication.getLateTolerance())); // Convert LocalTime to LocalDateTime
            Duration lateDuration = Duration.between(startTimeSchedule, selectedAttendance.getClockIn());

            // Triggered when employee clock in after endTime
            if (selectedAttendance.getClockIn().isAfter(currentDateTime.toLocalDate().atTime(selectedOvertimeApplication.getEndTime()))) {
                selectedAttendance.setStatus(AttendanceStatusEnum.ABSENT);
                selectedAttendance.setDeductionAmount(0);
                selectedAttendance.setDescription(selectedAttendance.getDescription() + "Additional Note: Absent!\n\n");

            } else if (currentDateTime.isBefore(currentDateTime.toLocalDate().atTime(selectedOvertimeApplication.getEndTime()))) {
                // Triggered when employee clock out earlier than it should be
                selectedAttendance.setStatus(AttendanceStatusEnum.ABSENT);
                selectedAttendance.setDeductionAmount(0);
                selectedAttendance.setDescription(selectedAttendance.getDescription() + "Additional Note: Early Clocked-Out!\n\n");

            } else {
                // Check clocked-in is after scheduled start time (consider as late).
                if (selectedAttendance.getClockIn().isAfter(startTimeSchedule)) {
                    selectedAttendance.setStatus(AttendanceStatusEnum.LATE);
                    selectedAttendance.setDeductionAmount(selectedStore.getLateClockInPenaltyAmount());
                    selectedAttendance.setLateInMinutes((int) lateDuration.toMinutes());
                    selectedAttendance.setDescription("Coming late: " + (lateDuration.toMinutes() + selectedOvertimeApplication.getLateTolerance()) + " minutes late!\n\n");
                } else {
                    selectedAttendance.setStatus(AttendanceStatusEnum.PRESENT);
                    selectedAttendance.setDescription("-");
                }
            }

            // In case if user do clock out, but they don't do break out (they do break in before)
            if (selectedAttendance.getBreakIn() != null && selectedAttendance.getBreakOut() == null) {
                long breakDuration = Duration.between(selectedAttendance.getBreakIn(), currentDateTime).toMinutes();
                if (breakDuration > (selectedStore.getBreakDuration() + selectedOvertimeApplication.getLateTolerance())) {
                    selectedAttendance.setDeductionAmount(selectedAttendance.getDeductionAmount() + selectedStore.getLateBreakOutPenaltyAmount());
                    selectedAttendance.setLateInMinutes((int) (selectedAttendance.getLateInMinutes() + (breakDuration - selectedStore.getBreakDuration())));
                    selectedAttendance.setDescription(Objects.toString(selectedAttendance.getDescription(), "") + selectedOvertimeApplication.getUser().getProfile().getName() + " is break more then " + selectedStore.getBreakDuration() + " minutes!\n\n");
                }
                selectedAttendance.setBreakOut(currentDateTime);

                selectedStore.setCurrentBreakCount(selectedStore.getCurrentBreakCount() - 1);
                storeRepository.save(selectedStore);
            }
            attendanceRepository.save(selectedAttendance);

        } catch (Exception e) {
            throw new InternalServerErrorException("Failed to clocked-out!");
        }
    }

    @Transactional
    @Override
    public void dailyClockIn(String username, LocalDateTime currentDateTime, MultipartFile photo, double lat, double lng) {
        User selectedUser = userRepository.findByUsernameAndIsActiveTrue(username)
                .orElseThrow(() -> new ResourceNotFoundException("User with username: " + username + " is not found!"));
        Store selectedStore = selectedUser.getStore();

        // Calculate and compare distance between user's location with store location and store radius.
        double calculateInMeters = GeolocationUtils.calculateDistance(lat, lng, selectedStore.getLat(), selectedStore.getLng());
        if (calculateInMeters > selectedStore.getRadius()) {
            throw new BadRequestException("Clocked-in failed: You are outside the allowed area!");
        }

        try {
            Attendance newAttendance = new Attendance();
            newAttendance.setStatus(AttendanceStatusEnum.ABSENT);
            newAttendance.setType(AttendanceTypeEnum.DAILY);
            newAttendance.setPhotoIn(photo.getBytes());
            newAttendance.setClockIn(currentDateTime);
            newAttendance.setDescription("");
            newAttendance.setDeductionAmount(0);
            newAttendance.setLateInMinutes(0);
            newAttendance.setUser(selectedUser);
            newAttendance.setStore(selectedStore);
            attendanceRepository.save(newAttendance);

        } catch (Exception e) {
            throw new InternalServerErrorException("Failed to clocked-in!");
        }
    }

    @Transactional
    @Override
    public void dailyClockOut(Long attendanceId, LocalDateTime currentDateTime, MultipartFile photo, double lat, double lng) {
        Attendance selectedAttendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance is not found!"));
        Store selectedStore = selectedAttendance.getStore();
        Schedule assignedSchedule = selectedAttendance.getUser().getProfile().getSchedule();
        User selectedUser = selectedAttendance.getUser();

        double calculateInMeters = GeolocationUtils.calculateDistance(lat, lng, selectedStore.getLat(), selectedStore.getLng());
        if (calculateInMeters > selectedStore.getRadius()) {
            throw new BadRequestException("Clocked-out failed: You are outside the allowed area!");
        }

        try {
            selectedAttendance.setPhotoOut(photo.getBytes());
            selectedAttendance.setClockOut(currentDateTime);
            selectedAttendance.setUpdatedDate(LocalDateTime.now());

            // Set schedule's start time plus late tolerate and calculate late duration between user's clocked-in time with schedule's time
            LocalDateTime startTimeSchedule = currentDateTime.toLocalDate().atTime(assignedSchedule.getStartTime().plusMinutes(assignedSchedule.getLateTolerance())); // Convert LocalTime to LocalDateTime
            Duration lateDuration = Duration.between(startTimeSchedule, selectedAttendance.getClockIn());

            // Triggered when employee clock in after endTime
            if (selectedAttendance.getClockIn().isAfter(currentDateTime.toLocalDate().atTime(assignedSchedule.getEndTime()))) {
                selectedAttendance.setStatus(AttendanceStatusEnum.ABSENT);
                selectedAttendance.setDeductionAmount(0);
                selectedAttendance.setDescription(selectedAttendance.getDescription() + "Additional Note: Absent!\n\n");

            } else if (currentDateTime.isBefore(currentDateTime.toLocalDate().atTime(assignedSchedule.getEndTime()))) {
                // Triggered when employee clock out earlier than it should be
                selectedAttendance.setStatus(AttendanceStatusEnum.ABSENT);
                selectedAttendance.setDeductionAmount(0);
                selectedAttendance.setDescription(selectedAttendance.getDescription() + "Additional Note: Early Clocked-Out!\n\n");

            } else {
                // Check clocked-in is after scheduled start time (consider as late).
                if (selectedAttendance.getClockIn().isAfter(startTimeSchedule)) {
                    selectedAttendance.setStatus(AttendanceStatusEnum.LATE);
                    selectedAttendance.setDeductionAmount(selectedStore.getLateClockInPenaltyAmount());
                    selectedAttendance.setLateInMinutes((int) lateDuration.toMinutes());
                    selectedAttendance.setDescription("Coming late: " + (lateDuration.toMinutes() + assignedSchedule.getLateTolerance()) + " minutes late!\n\n");
                } else {
                    selectedAttendance.setStatus(AttendanceStatusEnum.PRESENT);
                    selectedAttendance.setDescription("-");
                }
            }

            // In case if user do clock out, but they don't do break out (they do break in before)
            if (selectedAttendance.getBreakIn() != null && selectedAttendance.getBreakOut() == null) {
                long breakDuration = Duration.between(selectedAttendance.getBreakIn(), currentDateTime).toMinutes();
                if (breakDuration > (selectedStore.getBreakDuration() + assignedSchedule.getLateTolerance())) {
                    selectedAttendance.setDeductionAmount(selectedAttendance.getDeductionAmount() + selectedStore.getLateBreakOutPenaltyAmount());
                    selectedAttendance.setLateInMinutes((int) (selectedAttendance.getLateInMinutes() + (breakDuration - selectedUser.getStore().getBreakDuration())));
                    selectedAttendance.setDescription(Objects.toString(selectedAttendance.getDescription(), "") + selectedUser.getProfile().getName() + " is break more then " + selectedStore.getBreakDuration() + " minutes!\n\n");
                }
                selectedAttendance.setBreakOut(currentDateTime);

                selectedStore.setCurrentBreakCount(selectedStore.getCurrentBreakCount() - 1);
                storeRepository.save(selectedStore);
            }

            attendanceRepository.save(selectedAttendance);

        } catch (Exception e) {
            throw new InternalServerErrorException("Failed to clocked-out!");
        }
    }

    @Transactional
    @Override
    public void breakIn(Long attendanceId, LocalDateTime currentDateTime) {
        Attendance selectedAttendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance is not found!"));
        Store selectedStore = selectedAttendance.getUser().getStore();

        if (selectedStore.getCurrentBreakCount() >= selectedStore.getMaxBreakCount()) {
            throw new InternalServerErrorException("Break limit reached. Please wait until another employee returns!");
        }

        try {
            selectedAttendance.setBreakIn(currentDateTime);
            selectedAttendance.setUpdatedDate(LocalDateTime.now());
            attendanceRepository.save(selectedAttendance);

            selectedStore.setCurrentBreakCount(selectedStore.getCurrentBreakCount() + 1);
            storeRepository.save(selectedStore);

        } catch (Exception e) {
            throw new InternalServerErrorException("Failed to break-in!");
        }
    }

    @Transactional
    @Override
    public void breakOut(Long attendanceId, LocalDateTime currentDateTime) {
        Attendance selectedAttendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance is not found!"));
        User selectedUser = selectedAttendance.getUser();
        Store selectedStore = selectedUser.getStore();

        long breakDuration = Duration.between(selectedAttendance.getBreakIn(), currentDateTime).toMinutes();
        try {
            selectedAttendance.setBreakOut(currentDateTime);
            selectedAttendance.setUpdatedDate(LocalDateTime.now());

            if (breakDuration > (selectedStore.getBreakDuration() + selectedUser.getProfile().getSchedule().getLateTolerance())) {
                selectedAttendance.setDeductionAmount(selectedAttendance.getDeductionAmount() + selectedStore.getLateBreakOutPenaltyAmount());
                selectedAttendance.setLateInMinutes((int) (selectedAttendance.getLateInMinutes() + (breakDuration - selectedUser.getStore().getBreakDuration())));
                selectedAttendance.setDescription(Objects.toString(selectedAttendance.getDescription(), "") + selectedUser.getProfile().getName() + " is break more then " + selectedStore.getBreakDuration() + " minutes!\n\n");
            }
            attendanceRepository.save(selectedAttendance);

            selectedStore.setCurrentBreakCount(selectedStore.getCurrentBreakCount() - 1);
            storeRepository.save(selectedStore);

        } catch (Exception e) {
            throw new InternalServerErrorException("Failed to break-out!");
        }
    }

    @Override
    public Map<String, Object> getAttendance(Long attendanceId) {
        Attendance selectedAttendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance is not found!"));

        Salary currentSalary = salaryRepository.findLatestActiveSalaryByUserAndOptionalDate(selectedAttendance.getUser().getId(), selectedAttendance.getClockIn().toLocalDate())
                .orElseThrow(() -> new ResourceNotFoundException("Salary is not define yet!"));

        Profile selectedProfile = selectedAttendance.getUser().getProfile();

        AttendanceDTO attendanceDTO = modelMapper.map(selectedAttendance, AttendanceDTO.class);
        attendanceDTO.setUsername(selectedAttendance.getUser().getUsername());
        attendanceDTO.setRole(selectedAttendance.getUser().getRole().getName().name());
        attendanceDTO.setName(selectedProfile.getName());
        attendanceDTO.setIdProfile(selectedProfile.getId());
        attendanceDTO.setIdOvertime(selectedAttendance.getOvertimeApplication() != null
                ? selectedAttendance.getOvertimeApplication().getId()
                : null);
        Map<String, Object> response = new HashMap<>();
        response.put("baseSalary", currentSalary.getAmount());
        response.put("attendanceData", attendanceDTO);

        return response;
    }

    @Transactional
    @Override
    public void updateAttendance(Long attendanceId, String currentLoggedIn, String attendanceStatus, int deductionAmount, String attendanceDescription) {
        Attendance selectedAttendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance with id: " + attendanceId + " is not found!"));

        // CHANGE
        User selectedCurrentLoggedIn = userRepository.findByUsernameAndIsActiveTrue(currentLoggedIn)
                .orElseThrow(() -> new ResourceNotFoundException("User with username: " + currentLoggedIn + " is not found!"));

        try {
            if (selectedCurrentLoggedIn.getRole().getName() == RoleEnum.ROLE_ADMIN) {
                String activityDescription = selectedCurrentLoggedIn.getUsername() + " is updated a attendance issued by: " + selectedAttendance.getUser().getUsername() + " on date: " + selectedAttendance.getClockIn().toLocalDate();
                activityLogService.addActivityLog(selectedCurrentLoggedIn, "UPDATE", "Update Attendance", "Attendance", activityDescription);
            }
            selectedAttendance.setDescription(attendanceDescription);
            selectedAttendance.setStatus(AttendanceStatusEnum.valueOf(attendanceStatus));
            selectedAttendance.setDeductionAmount(deductionAmount);
            selectedAttendance.setUpdatedDate(LocalDateTime.now());
            attendanceRepository.save(selectedAttendance);

        } catch (Exception e) {
            throw new InternalServerErrorException("Failed to update attendance!");
        }
    }

    @Transactional
    @Override
    public void removeAttendance(Long attendanceId, String currentLoggedIn) {
        Attendance selectedAttendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance with id: " + attendanceId + " is not found!"));

        // CHANGE
        User selectedCurrentLoggedIn = userRepository.findByUsernameAndIsActiveTrue(currentLoggedIn)
                .orElseThrow(() -> new ResourceNotFoundException("User with username: " + currentLoggedIn + " is not found!"));

        try {
            if (selectedCurrentLoggedIn.getRole().getName() == RoleEnum.ROLE_ADMIN) {
                String activityDescription = selectedCurrentLoggedIn.getUsername() + " is delete a attendance issued by: " + selectedAttendance.getUser().getUsername() + " on date: " + selectedAttendance.getClockIn().toLocalDate();
                activityLogService.addActivityLog(selectedCurrentLoggedIn, "DELETE", "Remove Attendance", "Attendance", activityDescription);
            }
            attendanceRepository.delete(selectedAttendance);

        } catch (Exception e) {
            throw new InternalServerErrorException("Failed to delete the attendance!");
        }
    }

    @Override
    public List<AttendanceDTO> getAttendanceByMonthAndYear(Long userId, int month, int year) {
        User selectedUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id: " + userId + " is not found!"));

        List<Attendance> attendanceList = attendanceRepository.findByYearAndMonth(selectedUser.getId(), month, year);
        return attendanceList.stream().map(attendance -> modelMapper.map(attendance, AttendanceDTO.class)).toList();
    }

    @Override
    public byte[] getAttendancePhoto(Long attendanceId, String type) {
        Attendance selectedAttendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance with id: " + attendanceId + " is not found!"));
        return type.equals("IN") ? selectedAttendance.getPhotoIn() : selectedAttendance.getPhotoOut();
    }

    @Override
    public String getInAreaStatus(Long storeId, double lat, double lng) {
        Store selectedStore = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store with id: " + storeId + " is not found!"));

        double calculateInMeters = GeolocationUtils.calculateDistance(lat, lng, selectedStore.getLat(), selectedStore.getLng());

        return calculateInMeters > selectedStore.getRadius() ? "Out of Area" : "In the Area";
    }

    @Override
    public AttendanceDTO getTodayOvertimeSchedule(Long userId) {
        User selectedUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id: " + userId + " is not found!"));

        Attendance selectedAttendance = attendanceRepository.findOvertimeAttendanceByUserId(selectedUser.getId(), LocalDate.now())
                .orElseThrow(() -> new ResourceNotFoundException("Overtime Attendance with user id: " + userId + " is not found!"));
        return modelMapper.map(selectedAttendance, AttendanceDTO.class);
    }

    @Override
    public AttendanceDTO getTodayDailyAttendanceInfo(Long userId) {
        User selectedUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id: " + userId + " is not found!"));

        Attendance selectedAttendance = attendanceRepository.findByUserId(selectedUser.getId(), LocalDate.now())
                .orElseThrow(() -> new ResourceNotFoundException("Attendance with user id: " + selectedUser.getId() + " is not found!"));
        return modelMapper.map(selectedAttendance, AttendanceDTO.class);
    }

    @Override
    public AttendancePagination getTodayAttendances(Long storeId, String type, String keyword, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Store selectedStore = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store with id: " + storeId + " is not found!"));

        AttendanceTypeEnum typeEnum = Objects.equals(type, "All") ? null : AttendanceTypeEnum.valueOf(type);

        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);

        Page<Attendance> pageTodayAttendances = attendanceRepository.getTodayAttendances(selectedStore.getId(), typeEnum, "%" + keyword + "%", pageDetails);

        List<Attendance> todayAttendanceList = pageTodayAttendances.getContent();
        List<AttendanceDTO> attendanceDTOList = todayAttendanceList.stream().map(attendance -> {
            AttendanceDTO mappingResult = modelMapper.map(attendance, AttendanceDTO.class);
            mappingResult.setUsername(attendance.getUser().getUsername());
            mappingResult.setName(attendance.getUser().getProfile().getName());
            mappingResult.setRole(attendance.getUser().getRole().getName().name());
            return mappingResult;
        }).toList();

        AttendancePagination todayAttendancePagination = new AttendancePagination();
        todayAttendancePagination.setContent(attendanceDTOList);
        todayAttendancePagination.setPageNumber(pageTodayAttendances.getNumber());
        todayAttendancePagination.setPageSize(pageTodayAttendances.getSize());
        todayAttendancePagination.setTotalElements(pageTodayAttendances.getTotalElements());
        todayAttendancePagination.setTotalPages(pageTodayAttendances.getTotalPages());
        todayAttendancePagination.setLastPage(pageTodayAttendances.isLast());
        return todayAttendancePagination;
    }
}
