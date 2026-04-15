package com.smartlogistics.order.dto;

import com.smartlogistics.order.enums.OrderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateOrderRequest {

    @NotBlank(message = "From is required")
    @Size(min = 2, max = 50, message = "From must be between 2 and 50 characters")
    private String from;

    @NotBlank(message = "To is required")
    @Size(min = 2, max = 50, message = "To must be between 2 and 50 characters")
    private String to;

    @NotNull(message = "Order type is required")
    private OrderType orderType;

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

    public OrderType getOrderType() {
        return orderType;
    }

    public void setOrderType(OrderType orderType) {
        this.orderType = orderType;
    }
}
