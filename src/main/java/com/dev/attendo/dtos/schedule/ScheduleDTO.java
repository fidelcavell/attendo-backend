package com.dev.attendo.dtos.schedule;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleDTO {
    private Long id;
    private String name;
    private LocalTime startTime;
    private LocalTime endTime;
    private int lateTolerance;
}
