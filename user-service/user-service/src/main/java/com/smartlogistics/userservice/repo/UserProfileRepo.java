package com.smartlogistics.userservice.repo;

import com.smartlogistics.userservice.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserProfileRepo extends JpaRepository<UserProfile,Long> {

    Optional<UserProfile> findByUsername(String username);
    Optional<UserProfile> findByEmail(String email);
    List<UserProfile> findByCity(String city);
    List<UserProfile> findByUserRole(String role);
    Optional<UserProfile> findByAuthUserid(Long authUserid);
}
