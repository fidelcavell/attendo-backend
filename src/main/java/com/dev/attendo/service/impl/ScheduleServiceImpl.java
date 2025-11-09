package com.dev.attendo.service.impl;

import com.dev.attendo.dtos.schedule.ScheduleDTO;
import com.dev.attendo.exception.BadRequestException;
import com.dev.attendo.exception.InternalServerErrorException;
import com.dev.attendo.exception.ResourceNotFoundException;
import com.dev.attendo.model.Profile;
import com.dev.attendo.model.Store;
import com.dev.attendo.model.Schedule;
import com.dev.attendo.model.User;
import com.dev.attendo.repository.ProfileRepository;
import com.dev.attendo.repository.StoreRepository;
import com.dev.attendo.repository.ScheduleRepository;
import com.dev.attendo.repository.UserRepository;
import com.dev.attendo.service.ActivityLogService;
import com.dev.attendo.service.ScheduleService;
import com.dev.attendo.utils.enums.RoleEnum;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ScheduleServiceImpl implements ScheduleService {

    @Autowired
    StoreRepository storeRepository;

    @Autowired
    ScheduleRepository scheduleRepository;

    @Autowired
    ProfileRepository profileRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ActivityLogService activityLogService;

    @Autowired
    ModelMapper modelMapper;

    @Override
    public List<ScheduleDTO> getAllSchedule(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store with id: " + storeId + " is not found!"));

        return scheduleRepository.findByStoreId(store.getId()).stream().map(workSchedule -> modelMapper.map(workSchedule, ScheduleDTO.class)).toList();
    }

    @Override
    public ScheduleDTO getSchedule(Long scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule with id: " + scheduleId + " is not found!"));
        return modelMapper.map(schedule, ScheduleDTO.class);
    }

    @Transactional
    @Override
    public void addSchedule(Long storeId, String currentLoggedIn, ScheduleDTO scheduleDTO) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store with id: " + storeId + " is not found!"));
        User selectedCurrentLoggedIn = userRepository.findByUsernameAndIsActiveTrue(currentLoggedIn)
                .orElseThrow(() -> new ResourceNotFoundException("User with username: " + currentLoggedIn + " is not found!"));

        if (scheduleRepository.existsByNameAndStoreAndStartTimeAndEndTime(scheduleDTO.getName(), store, scheduleDTO.getStartTime(), scheduleDTO.getEndTime())) {
            throw new BadRequestException("Work Schedule with name " + scheduleDTO.getName() + " and range time: " + scheduleDTO.getStartTime().toString() + " - " + scheduleDTO.getEndTime().toString() + " is already exist!");
        }

        try {
            Schedule schedule = modelMapper.map(scheduleDTO, Schedule.class);
            schedule.setStore(store);
            scheduleRepository.save(schedule);

            if (selectedCurrentLoggedIn.getRole().getName() == RoleEnum.ROLE_ADMIN) {
                String activityDescription = selectedCurrentLoggedIn.getUsername() + " is added new schedule named: " + scheduleDTO.getName() + " with max late: " + scheduleDTO.getLateTolerance() + " minutes and time on: " + scheduleDTO.getStartTime() + " until " + scheduleDTO.getEndTime();
                activityLogService.addActivityLog(selectedCurrentLoggedIn, "ADD", "Add new schedule", "Schedule", activityDescription);
            }

        } catch (Exception e) {
            throw new InternalServerErrorException("Failed to add new schedule!");
        }
    }

    @Override
    public void updateSchedule(Long scheduleId, String currentLoggedIn, ScheduleDTO scheduleDTO) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule with id: " + scheduleId + " is not found!"));
        User selectedCurrentLoggedIn = userRepository.findByUsernameAndIsActiveTrue(currentLoggedIn)
                .orElseThrow(() -> new ResourceNotFoundException("User with username: " + currentLoggedIn + " is not found!"));

        try {
            schedule.setName(scheduleDTO.getName());
            schedule.setStartTime(scheduleDTO.getStartTime());
            schedule.setEndTime(scheduleDTO.getEndTime());
            schedule.setLateTolerance(scheduleDTO.getLateTolerance());
            schedule.setUpdatedDate(LocalDateTime.now());
            scheduleRepository.save(schedule);

            if (selectedCurrentLoggedIn.getRole().getName() == RoleEnum.ROLE_ADMIN) {
                String activityDescription = selectedCurrentLoggedIn.getUsername() + " is added new schedule called " + scheduleDTO.getName() + " with time on: " + scheduleDTO.getStartTime() + " until " + scheduleDTO.getEndTime();
                activityLogService.addActivityLog(selectedCurrentLoggedIn, "UPDATE", "Add new schedule", "Schedule", activityDescription);
            }

        } catch (Exception e) {
            throw new InternalServerErrorException("Failed to update work schedule!");
        }
    }

    @Override
    public void deleteSchedule(Long scheduleId) {
        Schedule selectedSchedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule with id: " + scheduleId + " is not found!"));

        List<Profile> profileList = profileRepository.findAllProfileByStoreAndScheduleAndIsActive(selectedSchedule.getStore().getId(), selectedSchedule.getId(), true);

        try {
            for (Profile profile : profileList) {
                profile.setSchedule(null);
            }
            profileRepository.saveAll(profileList);
            scheduleRepository.delete(selectedSchedule);


        } catch (Exception e) {
            throw new InternalServerErrorException("Failed to delete work schedule!");
        }
    }
}
