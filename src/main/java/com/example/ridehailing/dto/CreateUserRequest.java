package com.example.ridehailing.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateUserRequest(
		@NotBlank String name,
		@NotBlank String phone
) {
}
