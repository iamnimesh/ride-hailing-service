package com.example.ridehailing.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCouponRequest(
		@NotBlank String code,
		@NotNull @DecimalMin(value = "0.0", inclusive = false) @DecimalMax("100.0") BigDecimal discountPercentage
) {
}
