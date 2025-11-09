package com.dev.attendo.dtos.attendance;

import com.dev.attendo.utils.enums.AttendanceStatusEnum;
import com.dev.attendo.utils.enums.AttendanceTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceDTO {
    private Long id;
    private AttendanceTypeEnum type;
    private AttendanceStatusEnum status;
    private LocalDateTime clockIn;
    private LocalDateTime clockOut;
    private LocalDateTime breakIn;
    private LocalDateTime breakOut;
    private String description;
    private int deductionAmount;
    private int lateInMinutes;

    // Additional data (optional):
    private String username = null;
    private String name = null;
    private String role = null;
    private Long idProfile = null;
    private Long idOvertime = null;
}
