package com.smartlogistics.userservice.service;

import com.smartlogistics.userservice.dto.CreateProfileRequest;
import com.smartlogistics.userservice.dto.UpdateProfileRequest;
import com.smartlogistics.userservice.entity.UserProfile;
import com.smartlogistics.userservice.repo.UserProfileRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {
    private static final Logger logger = LoggerFactory.getLogger(ProfileService.class);

    public UserProfileRepo userProfileRepo;
    public ProfileService(UserProfileRepo userProfileRepo) {
        this.userProfileRepo = userProfileRepo;
    }

    public void createUserProfile(CreateProfileRequest request) {
        logger.info("createUserProfile");
        if (userProfileRepo.findByAuthUserid(request.getAuthUserId()).isPresent()) {
            throw new RuntimeException("User profile already exists");
        }
        UserProfile userProfile = new UserProfile();
        userProfile.setAuthUserId(request.getAuthUserId());
        userProfile.setEmail(request.getEmail());
        userProfile.setFirstName(request.getFirstName());
        userProfile.setLastName(request.getLastName());
        userProfile.setCity(request.getCity());
        userProfile.setPhoneNumber(request.getPhoneNumber());
        userProfile.setUserRole(request.getUserRole());
        userProfileRepo.save(userProfile);
        logger.info("createUserProfile success");
    }

    public void updateUserProfile(Long authUserId, UpdateProfileRequest request) {
        logger.info("updateUserProfile");
        UserProfile userProfile = userProfileRepo
                .findByAuthUserid(authUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

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
}
