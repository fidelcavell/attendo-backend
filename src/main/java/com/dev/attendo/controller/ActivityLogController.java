package com.dev.attendo.controller;

import com.dev.attendo.dtos.audit.ActivityLogPagination;
import com.dev.attendo.service.ActivityLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/activity-log")
@PreAuthorize("hasRole('OWNER')")
public class ActivityLogController {

    @Autowired
    ActivityLogService activityLogService;

    @GetMapping
    ResponseEntity<?> getAllActivityLogByUserId(
            @RequestParam(name = "store") Long storeId,
            @RequestParam(name = "method") String method,
            @RequestParam(name = "startDate", required = false) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) LocalDate endDate,
            @RequestParam(defaultValue = "", required = false) String keyword,
            @RequestParam(defaultValue = "0", required = false) Integer pageNumber,
            @RequestParam(defaultValue = "5", required = false) Integer pageSize,
            @RequestParam(defaultValue = "createdDate", required = false) String sortBy,
            @RequestParam(defaultValue = "desc", required = false) String sortOrder
    ) {
        ActivityLogPagination activityLogPagination = activityLogService.getAllActivityLog(storeId, keyword, method, startDate, endDate, pageNumber, pageSize, sortBy, sortOrder);
        return ResponseEntity.ok(activityLogPagination);
    }
}
