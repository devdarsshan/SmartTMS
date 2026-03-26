package com.smartlogistics.userservice.controller;

import com.smartlogistics.userservice.dto.CreateProfileRequest;
import com.smartlogistics.userservice.dto.UpdateProfileRequest;
import com.smartlogistics.userservice.entity.UserProfile;
import com.smartlogistics.userservice.service.ProfileService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController()
@RequestMapping(path = "/user")
public class UserController {

    public ProfileService profileService;

    public UserController(ProfileService profileService) {
        this.profileService = profileService;
    }

    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    @PostMapping(path = "/createProfile")
    public ResponseEntity<Void> createUserProfile(@Valid @RequestBody CreateProfileRequest request) {
        log.info("Entering CreateUserProfile Controller");
        profileService.createUserProfile(request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/updateProfile/{authUserId}")
    public ResponseEntity<Void> updateUserProfile(
            @PathVariable Long authUserId,
            @RequestBody UpdateProfileRequest request) {
        log.info("Entering UpdateUserProfile Controller");
        request.setAuthUserId(authUserId);
        profileService.updateUserProfile(authUserId,request);
        return ResponseEntity.ok().build();
    }

    @GetMapping(path = "/fetchProfile/{authUserId}")
    public ResponseEntity<UserProfile> fetchUserProfile(
            @PathVariable Long authUserId) {
        log.info("Entering FetchUserProfile Controller");
        return profileService.fetchUserProfile(authUserId);
    }

    @GetMapping(path = "/fetchProfilesByRole/{role}")
    public ResponseEntity<List<UserProfile>> fetchUserProfilesByRoles(
            @PathVariable String role) {
        log.info("Entering FetchUserProfilesByRoles Controller");
        return profileService.fetchUserProfilesByRoles(role);
    }
}
