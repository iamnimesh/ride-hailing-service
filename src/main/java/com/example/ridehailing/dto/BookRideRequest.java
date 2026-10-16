package com.example.ridehailing.dto;

import com.example.ridehailing.entity.CarType;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record BookRideRequest(
		@NotNull Long userId,
		@NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double pickupLatitude,
		@NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double pickupLongitude,
		@NotNull CarType carType,
		String couponCode
) {
}
