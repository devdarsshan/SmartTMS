package com.smartlogistics.auth.dto;

public class AuthResponse {

    public AuthResponse(String token, long userId) {
        this.token = token;
        this.userId = userId;
    }

    private String token;
    private long userId;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }
}
