package com.smartlogistics.userservice.dto;

import com.smartlogistics.userservice.enums.DriverStatus;
import com.smartlogistics.userservice.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class CreateProfileRequest {

    @NotNull(message = "User Id is required")
    private Long authUserId;
    @NotBlank(message = "Email is required")
    @Email
    private String email;
    @NotBlank(message = "Firstname is required")
    private String firstName;
    private String lastName;
    private String phoneNumber;
    @NotBlank(message = "Role is required")
    @Pattern(regexp = "^(ADMIN|USER|DRIVER|DISPATCHER)$", message = "Role must be one of: ADMIN, USER, DRIVER, DISPATCHER")
    private UserRole userRole;
    private String city;
    private DriverStatus driverStatus = DriverStatus.ACTIVE;

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Long getAuthUserId() {
        return authUserId;
    }

    public void setAuthUserId(Long authUserId) {
        this.authUserId = authUserId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public UserRole getUserRole() {
        return userRole;
    }

    public void setUserRole(UserRole userRole) {
        this.userRole = userRole;
    }

    public DriverStatus getDriverStatus() {
        return driverStatus;
    }

    public void setDriverStatus(DriverStatus driverStatus) {
        this.driverStatus = driverStatus;
    }
}
