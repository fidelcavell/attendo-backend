package com.dev.attendo.dtos.overtime;

import com.dev.attendo.utils.enums.ApprovalStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OvertimeDTO {
    private Long id;
    private ApprovalStatusEnum status;
    private LocalDate overtimeDate;
    private String description;
    private int overtimePay = 0;
    private LocalTime startTime;
    private LocalTime endTime;

    // Additional
    private String assignedTime;
    private String issuedBy;
    private String approvedBy;
}
