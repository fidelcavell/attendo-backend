package com.dev.attendo.service;

import com.dev.attendo.dtos.schedule.ScheduleDTO;

import java.util.List;

public interface ScheduleService {

    List<ScheduleDTO> getAllSchedule(Long storeId);

    ScheduleDTO getSchedule(Long scheduleId);

    void addSchedule(Long storeId, String currentLoggedIn, ScheduleDTO scheduleDTO);

    void updateSchedule(Long scheduleId, String currentLoggedIn, ScheduleDTO scheduleDTO);

    void deleteSchedule(Long scheduleId);
}
