package com.dev.attendo.controller;

import com.dev.attendo.dtos.schedule.ScheduleDTO;
import com.dev.attendo.security.response.MessageResponse;
import com.dev.attendo.service.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/schedule")
@PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
public class ScheduleController {

    @Autowired
    ScheduleService scheduleService;

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/store/{storeId}")
    public ResponseEntity<?> getAllSchedule(@PathVariable Long storeId) {
        return ResponseEntity.ok(scheduleService.getAllSchedule(storeId));
    }

    @PostMapping("/{storeId}")
    public ResponseEntity<?> addSchedule(@PathVariable Long storeId,@RequestParam String currentLoggedIn, @RequestBody ScheduleDTO scheduleDTO) {
        scheduleService.addSchedule(storeId, currentLoggedIn, scheduleDTO);
        return ResponseEntity.ok(new MessageResponse(true, "Data jadwal kerja baru berhasil ditambahkan!"));
    }

    @PutMapping("/{scheduleId}")
    public ResponseEntity<?> updateSchedule(@PathVariable Long scheduleId, @RequestParam String currentLoggedIn, @RequestBody ScheduleDTO scheduleDTO) {
        scheduleService.updateSchedule(scheduleId, currentLoggedIn, scheduleDTO);
        return ResponseEntity.ok(new MessageResponse(true, "Data jadwal kerja berhasil diubah!"));
    }

    @PreAuthorize("hasRole('OWNER')")
    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<?> deleteSchedule(@PathVariable Long scheduleId) {
        scheduleService.deleteSchedule(scheduleId);
        return ResponseEntity.ok(new MessageResponse(true, "Data jadwal kerja berhasil dihapus!"));
    }
}
