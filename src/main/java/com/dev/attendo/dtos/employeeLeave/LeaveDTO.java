package com.dev.attendo.dtos.employeeLeave;

import com.dev.attendo.utils.enums.ApprovalStatusEnum;
import com.dev.attendo.utils.enums.LeaveTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveDTO {
    private int id;
    private ApprovalStatusEnum status;
    private LeaveTypeEnum type;
    private LocalDate startDate;
    private LocalDate endDate;
    private String description;

    // Additional
    private String issuedBy;
    private String approvedBy;
}
