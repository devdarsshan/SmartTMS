package com.smartlogistics.userservice.service;

import com.smartlogistics.userservice.dto.CreateProfileRequest;
import com.smartlogistics.userservice.dto.UpdateProfileRequest;
import com.smartlogistics.userservice.entity.UserProfile;
import com.smartlogistics.userservice.exception.ProfileAlreadyExistsException;
import com.smartlogistics.userservice.exception.UserprofileNotFoundException;
import com.smartlogistics.userservice.repo.UserProfileRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Profile Service with Redis Caching
 * Implements caching strategy as per Redis_Caching_Strategy.md:
 * - User profiles: 1 hour cache
 * - Role lists: 30 minutes cache
 * - Role checks: 1 hour cache
 */
@Service
public class ProfileService {
    private static final Logger logger = LoggerFactory.getLogger(ProfileService.class);

    private final UserProfileRepo userProfileRepo;
    private final CacheManager cacheManager;

    public ProfileService(UserProfileRepo userProfileRepo, CacheManager cacheManager) {
        this.userProfileRepo = userProfileRepo;
        this.cacheManager = cacheManager;
    }

    public void createUserProfile(String userIdHeader, CreateProfileRequest request) {
        logger.info("Creating user profile for authUserId from header");
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
        
        // Evict role-based cache as new user is added to a role
        evictUsersByRoleCache(userProfile.getUserRole());
        
        logger.info("User profile created successfully for authUserId: {}, role: {}", authUserId, userProfile.getUserRole());
    }

    /**
     * Updates user profile and evicts related caches.
     * Evicts both the user profile cache and role-based cache if role changes.
     */
    @Caching(evict = {
        @CacheEvict(value = "userProfiles", key = "#authUserId"),
        @CacheEvict(value = "userRoleCheck", key = "#authUserId")
    })
    public void updateUserProfile(Long authUserId, UpdateProfileRequest request) {
        logger.info("Updating user profile for authUserId: {}", authUserId);
        UserProfile userProfile = userProfileRepo
                .findByAuthUserId(authUserId)
                .orElseThrow(() -> new UserprofileNotFoundException("User not found"));

        String oldRole = userProfile.getUserRole();
        boolean roleChanged = false;

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

        if (request.getUserRole() != null && !request.getUserRole().equals(oldRole)) {
            userProfile.setUserRole(request.getUserRole());
            roleChanged = true;
        }

        if (request.getCity() != null) {
            userProfile.setCity(request.getCity());
        }

        userProfileRepo.save(userProfile);

        // If role changed, evict both old and new role caches
        if (roleChanged) {
            evictUsersByRoleCache(oldRole);
            evictUsersByRoleCache(userProfile.getUserRole());
            logger.info("User role changed from {} to {}, caches evicted", oldRole, userProfile.getUserRole());
        }

        logger.info("User profile updated successfully for authUserId: {}", authUserId);
    }

    /**
     * Fetches user profile by authUserId with caching.
     * Cache key: user:profile:{authUserId}
     * TTL: 1 hour (as per strategy)
     */
    @Cacheable(value = "userProfiles", key = "#authUserId", unless = "#result == null")
    public UserProfile fetchUserProfile(Long authUserId) {
        logger.info("Fetching user profile from database for authUserId: {}", authUserId);
        UserProfile userProfile = userProfileRepo.findByAuthUserId(authUserId)
                    .orElseThrow(() -> new UserprofileNotFoundException("User not found"));
        logger.info("User profile fetched successfully for authUserId: {}", authUserId);
        return userProfile;
    }

    /**
     * Fetches users by role with caching.
     * Cache key: user:role:{roleName}:list
     * TTL: 30 minutes (as per strategy)
     */
    @Cacheable(value = "usersByRole", key = "#userRole", unless = "#result == null || #result.isEmpty()")
    public List<UserProfile> fetchUserProfilesByRoles(String userRole) {
        logger.info("Fetching user profiles from database for role: {}", userRole);
        List<UserProfile> userProfiles = userProfileRepo.findByUserRole(userRole);
        logger.info("{} users with role {} found", userProfiles.size(), userRole);
        return userProfiles;
    }

    /**
     * Helper method to evict role-based cache when users are added/removed from roles.
     */
    public void evictUsersByRoleCache(String userRole) {
        Cache cache = cacheManager.getCache("usersByRole");
        if (cache == null) {
            logger.debug("Cache 'usersByRole' not configured; skipping eviction for role: {}", userRole);
            return;
        }
        logger.debug("Evicting 'usersByRole' cache for role: {}", userRole);
        cache.evict(userRole);
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
