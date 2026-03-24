package com.smartlogistics.userservice.controller;

import com.smartlogistics.userservice.dto.CreateProfileRequest;
import com.smartlogistics.userservice.dto.UpdateProfileRequest;
import com.smartlogistics.userservice.service.ProfileService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController()
@RequestMapping(path = "/user")
public class ProfileController {

    public ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    private static final Logger log = LoggerFactory.getLogger(ProfileController.class);
    @PostMapping(path = "/createProfile")
    public ResponseEntity<Void> createUserProfile(@Valid @RequestBody CreateProfileRequest request) {
        log.info("createUserProfile");
        return ResponseEntity.ok().build();
    }

    @PutMapping("/updateProfile/{authUserId}")
    public ResponseEntity<Void> updateUserProfile(
            @PathVariable Long authUserId,
            @RequestBody UpdateProfileRequest request) {

        request.setAuthUserId(authUserId); // enforce path consistency
        profileService.updateUserProfile(authUserId,request);

        return ResponseEntity.ok().build();
    }
}
