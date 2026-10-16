package com.example.ridehailing.dto;

import com.example.ridehailing.entity.User;

public record UserResponse(Long id, String name, String phone) {

	public static UserResponse from(User user) {
		return new UserResponse(user.getId(), user.getName(), user.getPhone());
	}

}
