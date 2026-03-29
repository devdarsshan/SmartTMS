package com.smartlogistics.userservice.service;

import com.smartlogistics.userservice.dto.CreateProfileRequest;
import com.smartlogistics.userservice.dto.UpdateProfileRequest;
import com.smartlogistics.userservice.entity.UserProfile;
import com.smartlogistics.userservice.exception.ProfileAlreadyExistsException;
import com.smartlogistics.userservice.exception.UserprofileNotFoundException;
import com.smartlogistics.userservice.repo.UserProfileRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProfileService {
    private static final Logger logger = LoggerFactory.getLogger(ProfileService.class);

    public UserProfileRepo userProfileRepo;
    public ProfileService(UserProfileRepo userProfileRepo) {
        this.userProfileRepo = userProfileRepo;
    }

    public void createUserProfile(String userIdHeader, CreateProfileRequest request) {
        logger.info("createUserProfile");
        Long authUserId = parseAuthUserId(userIdHeader);
        if (userProfileRepo.findByAuthUserId(authUserId).isPresent()) {
            throw new ProfileAlreadyExistsException("User profile already exists");
        }
        UserProfile userProfile = new UserProfile();
        userProfile.setAuthUserId(authUserId);
        userProfile.setEmail(request.getEmail());
        userProfile.setFirstName(request.getFirstName());
        userProfile.setLastName(request.getLastName());
        userProfile.setCity(request.getCity());
        userProfile.setPhoneNumber(request.getPhoneNumber());
        userProfile.setUserRole(request.getUserRole().toString());
        userProfileRepo.save(userProfile);
        logger.info("createUserProfile success");
    }

    public void updateUserProfile(Long authUserId, UpdateProfileRequest request) {
        logger.info("updateUserProfile");
        UserProfile userProfile = userProfileRepo
                .findByAuthUserId(authUserId)
                .orElseThrow(() -> new UserprofileNotFoundException("User not found"));

        if (request.getEmail() != null) {
            userProfile.setEmail(request.getEmail());
        }

        if (request.getFirstName() != null) {
            userProfile.setFirstName(request.getFirstName());
        }

        if (request.getLastName() != null) {
            userProfile.setLastName(request.getLastName());
        }

        if (request.getPhoneNumber() != null) {
            userProfile.setPhoneNumber(request.getPhoneNumber());
        }

        if (request.getUserRole() != null) {
            userProfile.setUserRole(request.getUserRole());
        }

        if (request.getCity() != null) {
            userProfile.setCity(request.getCity());
        }

        userProfileRepo.save(userProfile);
        logger.info("updateUserProfile success");
    }

    public ResponseEntity<UserProfile> fetchUserProfile(Long authUserId) {
        logger.info("fetchUserProfile");
        UserProfile userProfile =  userProfileRepo.findByAuthUserId(authUserId)
                    .orElseThrow(() -> new UserprofileNotFoundException("User not found"));
        return ResponseEntity.ok().body(userProfile);
    }

    public ResponseEntity<List<UserProfile>> fetchUserProfilesByRoles(String userRole) {
        logger.info("fetchAllUserProfiles-service");
        List<UserProfile> userProfiles = userProfileRepo.findByUserRole(userRole);
        logger.info("{} users with role {} found",userProfiles.size(), userRole);
        return ResponseEntity.ok().body(userProfiles);
    }

    private Long parseAuthUserId(String userIdHeader) {
        if (userIdHeader == null || userIdHeader.trim().isEmpty()) {
            throw new IllegalArgumentException("X-User-Id header is required");
        }
        try {
            return Long.parseLong(userIdHeader);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("X-User-Id header must be a valid number");
        }
    }
}
