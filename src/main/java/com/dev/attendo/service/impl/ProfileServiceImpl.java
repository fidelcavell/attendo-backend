package com.dev.attendo.service.impl;

import com.dev.attendo.dtos.profile.ProfileDTO;
import com.dev.attendo.dtos.profile.ProfilePagination;
import com.dev.attendo.exception.BadRequestException;
import com.dev.attendo.exception.InternalServerErrorException;
import com.dev.attendo.exception.ResourceNotFoundException;
import com.dev.attendo.model.*;
import com.dev.attendo.repository.*;
import com.dev.attendo.service.ActivityLogService;
import com.dev.attendo.service.ProfileService;
import com.dev.attendo.utils.enums.RoleEnum;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ProfileServiceImpl implements ProfileService {

    @Autowired
    ModelMapper modelMapper;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ProfileRepository profileRepository;

    @Autowired
    ScheduleRepository scheduleRepository;

    @Autowired
    ActivityLogService activityLogService;

    @Override
    public ProfileDTO getProfile(Long profileId) {
        Profile selectedProfile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile is not found!"));

        ProfileDTO mappingResult = modelMapper.map(selectedProfile, ProfileDTO.class);
        mappingResult.setIdUser(selectedProfile.getUser().getId());
        mappingResult.setUsername(selectedProfile.getUser().getUsername());
        mappingResult.setEmail(selectedProfile.getUser().getEmail());
        mappingResult.setRoleName(selectedProfile.getUser().getRole().getName().name());

        if (selectedProfile.getSchedule() != null) {
            mappingResult.setIdSchedule(selectedProfile.getSchedule().getId());
        }
        return mappingResult;
    }

    @Override
    public ProfilePagination getAllAssociateEmployee(String currentUser, Long storeId, String keyword, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        User currentLoggedInUser = userRepository.findByUsernameAndIsActiveTrue(currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("User with username: " + currentUser + " is not found!"));

        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Page<Profile> pageProfiles = profileRepository.getAllAssociateProfile(currentLoggedInUser.getRole().getName().name(), storeId, '%' + keyword + '%', pageDetails);

        List<Profile> profileList = pageProfiles.getContent();
        List<ProfileDTO> profileDTOList = profileList.stream().map(profile -> {
            ProfileDTO mappingResult = modelMapper.map(profile, ProfileDTO.class);
            mappingResult.setIdUser(profile.getUser().getId());
            mappingResult.setUsername(profile.getUser().getUsername());
            mappingResult.setEmail(profile.getUser().getEmail());
            mappingResult.setRoleName(profile.getUser().getRole().getName().name());
            if (profile.getSchedule() != null) {
                mappingResult.setIdSchedule(profile.getSchedule().getId());
            }
            return mappingResult;
        }).toList();

        ProfilePagination profilePagination = new ProfilePagination();
        profilePagination.setContent(profileDTOList);
        profilePagination.setPageNumber(pageProfiles.getNumber());
        profilePagination.setPageSize(pageProfiles.getSize());
        profilePagination.setTotalElements(pageProfiles.getTotalElements());
        profilePagination.setTotalPages(pageProfiles.getTotalPages());
        profilePagination.setLastPage(pageProfiles.isLast());
        return profilePagination;
    }

    @Transactional
    @Override
    public void addProfile(String username, ProfileDTO profileDTO, MultipartFile profilePicture) {
        User user = userRepository.findByUsernameAndIsActiveTrue(username)
                .orElseThrow(() -> new ResourceNotFoundException("User with username: " + username + " is not found!"));

        if (user.getProfile() != null) {
            throw new BadRequestException("User with username: " + username + " is already has profile data!");
        }

        Profile newAddedProfile = modelMapper.map(profileDTO, Profile.class);
        Profile existProfile = profileRepository.findByName(newAddedProfile.getName());
        if (existProfile != null) {
            throw new BadRequestException("Profile with name: " + newAddedProfile.getName() + " is already exist!");
        }

        try {
            newAddedProfile.setProfilePicture(profilePicture.getBytes());
            profileRepository.save(newAddedProfile);

            user.setProfile(newAddedProfile);
            userRepository.save(user);
        } catch (Exception e) {
            throw new InternalServerErrorException("Failed to add new user profile!");
        }
    }

    @Transactional
    @Override
    public void updateProfile(Long profileId, ProfileDTO profileDTO, MultipartFile profilePicture) {
        Profile selectedProfile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile is not found!"));
        try {
            selectedProfile.setName(profileDTO.getName());
            selectedProfile.setAddress(profileDTO.getAddress());
            selectedProfile.setPhoneNumber(profileDTO.getPhoneNumber());
            selectedProfile.setBirthDate(profileDTO.getBirthDate());
            selectedProfile.setGender(profileDTO.getGender());
            selectedProfile.setProfilePicture(profilePicture.getBytes());
            selectedProfile.setUpdatedDate(LocalDateTime.now());
            profileRepository.save(selectedProfile);
        } catch (Exception e) {
            throw new InternalServerErrorException("Failed to update user profile! " + e.getMessage());
        }
    }

    @Override
    public byte[] getProfilePicture(Long profileId) {
        Profile selectedProfile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile is not found!"));
        if (selectedProfile.getProfilePicture() == null) {
            throw new ResourceNotFoundException("Profile Picture is not available!");
        }
        return selectedProfile.getProfilePicture();
    }

    @Override
    public void updateWorkSchedule(Long profileId, Long scheduleId, String currentUser) {
        Profile selectedProfile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile with id: " + profileId + " is not found!"));
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Work Schedule with id: " + scheduleId + " is not found!"));
        User currentLoggedIn = userRepository.findByUsernameAndIsActiveTrue(currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("User with username: " + currentUser + " is not found!"));

        try {
            selectedProfile.setSchedule(schedule);
            profileRepository.save(selectedProfile);

            if (currentLoggedIn.getRole().getName() == RoleEnum.ROLE_ADMIN) {
                String scheduleTime = schedule.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")) + " WIB" + " - " + schedule.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")) + " WIB";

                String activityDescription = currentLoggedIn.getUsername() + " changes work schedule for: " + selectedProfile.getUser().getUsername() + " with schedule: " + scheduleTime;
                activityLogService.addActivityLog(currentLoggedIn, "UPDATE", "Update Work Schedule", "Profile", activityDescription);
            }

        } catch (Exception e) {
            throw new InternalServerErrorException("Failed to update user's work schedule!");
        }
    }
}

