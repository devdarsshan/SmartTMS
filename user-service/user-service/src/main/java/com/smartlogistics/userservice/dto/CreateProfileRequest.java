package com.smartlogistics.userservice.dto;

import com.smartlogistics.userservice.enums.DriverStatus;
import com.smartlogistics.userservice.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateProfileRequest {

    @NotBlank(message = "Email is required")
    @Email
    private String email;
    @NotBlank(message = "Firstname is required")
    private String firstName;
    private String lastName;
    private String phoneNumber;
    @NotNull(message = "Role is required")
    private UserRole userRole;
    private String city;
    private DriverStatus driverStatus = DriverStatus.ACTIVE;

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
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
