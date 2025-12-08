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
                .orElseThrow(() -> new ResourceNotFoundException("Data toko dengan id: " + storeId + " tidak ditemukan!"));

        return scheduleRepository.findByStoreId(store.getId()).stream().map(workSchedule -> modelMapper.map(workSchedule, ScheduleDTO.class)).toList();
    }

    @Transactional
    @Override
    public void addSchedule(Long storeId, String currentLoggedIn, ScheduleDTO scheduleDTO) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Data toko dengan id: " + storeId + " tidak ditemukan!"));
        User selectedCurrentLoggedIn = userRepository.findByUsernameAndIsActiveTrue(currentLoggedIn)
                .orElseThrow(() -> new ResourceNotFoundException("User dengan username: " + currentLoggedIn + " tidak ditemukan!"));

        if (scheduleRepository.existsByNameAndStoreAndStartTimeAndEndTime(scheduleDTO.getName(), store, scheduleDTO.getStartTime(), scheduleDTO.getEndTime())) {
            throw new BadRequestException("Data jadwal kerja dengan nama " + scheduleDTO.getName() + " dan dengan waktu: " + scheduleDTO.getStartTime().toString() + " - " + scheduleDTO.getEndTime().toString() + " telah tersedia!");
        }

        try {
            Schedule schedule = modelMapper.map(scheduleDTO, Schedule.class);
            schedule.setStore(store);
            scheduleRepository.save(schedule);

            if (selectedCurrentLoggedIn.getRole().getName() == RoleEnum.ROLE_ADMIN) {
                String activityDescription = selectedCurrentLoggedIn.getUsername() + " menambahkan jadwal kerja baru dengan nama: " + scheduleDTO.getName() + ", batas keterlambatan: " + scheduleDTO.getLateTolerance() + " minutes dan dengan waktu: " + scheduleDTO.getStartTime() + " hingga " + scheduleDTO.getEndTime();
                activityLogService.addActivityLog(selectedCurrentLoggedIn, "ADD", "Add new schedule", "Schedule", activityDescription);
            }

        } catch (Exception e) {
            throw new InternalServerErrorException("Gagal menambahkan data jadwal kerja baru!");
        }
    }

    @Override
    public void updateSchedule(Long scheduleId, String currentLoggedIn, ScheduleDTO scheduleDTO) {
        Schedule selectedSchedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Data jadwal kerja dengan id: " + scheduleId + " tidak ditemukan!"));
        User selectedCurrentLoggedIn = userRepository.findByUsernameAndIsActiveTrue(currentLoggedIn)
                .orElseThrow(() -> new ResourceNotFoundException("User dengan username: " + currentLoggedIn + " tidak ditemukan!"));

        try {
            selectedSchedule.setName(scheduleDTO.getName());
            selectedSchedule.setStartTime(scheduleDTO.getStartTime());
            selectedSchedule.setEndTime(scheduleDTO.getEndTime());
            selectedSchedule.setLateTolerance(scheduleDTO.getLateTolerance());
            selectedSchedule.setUpdatedDate(LocalDateTime.now());
            scheduleRepository.save(selectedSchedule);

            if (selectedCurrentLoggedIn.getRole().getName() == RoleEnum.ROLE_ADMIN) {
                String activityDescription = selectedCurrentLoggedIn.getUsername() + " mengubah data jadwal kerja " + scheduleDTO.getName() + " dengan waktu: " + scheduleDTO.getStartTime() + " hingga " + scheduleDTO.getEndTime();
                activityLogService.addActivityLog(selectedCurrentLoggedIn, "UPDATE", "Add new schedule", "Schedule", activityDescription);
            }

        } catch (Exception e) {
            throw new InternalServerErrorException("Gagal mengubah data jadwal kerja!");
        }
    }

    @Override
    public void deleteSchedule(Long scheduleId) {
        Schedule selectedSchedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Data jadwal kerja dengan id: " + scheduleId + " tidak ditemukan!"));

        List<Profile> profileList = profileRepository.findAllProfileByStoreAndScheduleAndIsActive(selectedSchedule.getStore().getId(), selectedSchedule.getId(), true);
        try {
            for (Profile profile : profileList) {
                profile.setSchedule(null);
            }
            profileRepository.saveAll(profileList);
            scheduleRepository.delete(selectedSchedule);

        } catch (Exception e) {
            throw new InternalServerErrorException("Gagal menghapus data jadwal kerja!");
        }
    }
}
