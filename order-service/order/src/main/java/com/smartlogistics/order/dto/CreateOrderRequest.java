package com.smartlogistics.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateOrderRequest {

    @NotBlank(message = "From is required")
    @Size(min = 2, max = 50, message = "From must be between 2 and 50 characters")
    private String from;

    @NotBlank(message = "To is required")
    @Size(min = 2, max = 50, message = "To must be between 2 and 50 characters")
    private String to;

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }
}
