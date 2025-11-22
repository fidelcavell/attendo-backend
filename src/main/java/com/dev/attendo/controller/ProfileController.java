package com.dev.attendo.controller;

import com.dev.attendo.dtos.profile.ProfileDTO;
import com.dev.attendo.dtos.profile.ProfilePagination;
import com.dev.attendo.security.response.MessageResponse;
import com.dev.attendo.service.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    @Autowired
    ProfileService profileService;

    @GetMapping("/{profileId}")
    public ResponseEntity<ProfileDTO> getProfile(@PathVariable Long profileId) {
        ProfileDTO profileData = profileService.getProfile(profileId);
        return ResponseEntity.ok(profileData);
    }

    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @GetMapping("/employees")
    public ResponseEntity<ProfilePagination> getAllAssociateEmployees(
            @RequestParam(name = "store") Long storeId,
            @RequestParam String currentUser,
            @RequestParam(defaultValue = "", required = false) String keyword,
            @RequestParam(defaultValue = "0", required = false) Integer pageNumber,
            @RequestParam(defaultValue = "5", required = false) Integer pageSize,
            @RequestParam(defaultValue = "updatedDate", required = false) String sortBy,
            @RequestParam(defaultValue = "desc", required = false) String sortOrder
    ) {
        ProfilePagination profilePagination = profileService.getAllAssociateEmployee(currentUser, storeId, keyword, pageNumber, pageSize, sortBy, sortOrder);
        return ResponseEntity.ok(profilePagination);
    }

    @PostMapping(
            value = "/{username}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> addProfile(
            @PathVariable String username,
            @RequestPart("profileDTO") ProfileDTO profileDTO,
            @RequestPart(value = "profilePicture", required = false) MultipartFile profilePicture
    ) throws IOException {
        profileService.addProfile(username, profileDTO, profilePicture);
        return ResponseEntity.ok(
                new MessageResponse(true, username + "'s profile has been created!")
        );
    }

    @PutMapping(
            value = "/{profileId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> updateProfile(
            @PathVariable Long profileId,
            @RequestPart("profileDTO") ProfileDTO profileDTO,
            @RequestPart(value = "profilePicture", required = false) MultipartFile profilePicture
    ) {
        profileService.updateProfile(profileId, profileDTO, profilePicture);
        return ResponseEntity.ok(new MessageResponse(true, "User's profile has been updated!"));
    }

    @GetMapping("/{profileId}/profile-picture")
    public ResponseEntity<byte[]> getProfilePicture(@PathVariable Long profileId) {
        byte[] profilePicture = profileService.getProfilePicture(profileId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "image/jpeg")
                .body(profilePicture);
    }

    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @PutMapping("/assign-schedule/{profileId}")
    public ResponseEntity<?> updateUserWorkSchedule(
            @PathVariable Long profileId,
            @RequestParam(name = "schedule") Long scheduleId,
            @RequestParam String currentUser
    ) {
        profileService.updateWorkSchedule(profileId, scheduleId, currentUser);
        return ResponseEntity.ok(new MessageResponse(true, "New Schedule is has been assigned!"));
    }
}
