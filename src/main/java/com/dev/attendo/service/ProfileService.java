package com.dev.attendo.service;

import com.dev.attendo.dtos.profile.ProfileDTO;
import com.dev.attendo.dtos.profile.ProfilePagination;
import com.dev.attendo.utils.enums.RoleEnum;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ProfileService {

    ProfileDTO getProfile(Long userId);

    ProfilePagination getAllAssociateEmployee(String currentUser, Long storeId, String keyword, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);

    void addProfile(String username, ProfileDTO profileDTO, MultipartFile profilePicture) throws IOException;

    void updateProfile(Long profileId, ProfileDTO profileDTO, MultipartFile profilePicture);

    byte[] getProfilePicture(Long profileId);

    void updateWorkSchedule(Long profileId, Long scheduleId, String currentUser);
}
