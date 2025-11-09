package com.dev.attendo.controller;

import com.dev.attendo.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/report")
@PreAuthorize("hasRole('OWNER')")
public class ReportController {

    @Autowired
    ReportService reportService;

    @GetMapping("/expenses/{storeId}")
    public ResponseEntity<?> getExpensesReport(@PathVariable Long storeId, @RequestParam int period) {
        return ResponseEntity.ok(reportService.getExpensesReport(storeId, period));
    }

    @GetMapping("/frequently-late/{storeId}")
    public ResponseEntity<?> getFrequentlyLateReport(@PathVariable Long storeId, @RequestParam int period) {
        return ResponseEntity.ok(reportService.getFrequentlyLateReport(storeId, period));
    }

    @GetMapping("/leave-vs-overtime/{storeId}")
    public ResponseEntity<?> getLeaveVsOvertimeReport(@PathVariable Long storeId, @RequestParam int period) {
        return ResponseEntity.ok(reportService.getLeaveVsOvertimeReport(storeId, period));
    }
}
