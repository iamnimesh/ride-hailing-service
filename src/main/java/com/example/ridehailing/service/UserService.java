package com.example.ridehailing.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ridehailing.dto.CreateUserRequest;
import com.example.ridehailing.dto.UserResponse;
import com.example.ridehailing.entity.User;
import com.example.ridehailing.exception.ResourceNotFoundException;
import com.example.ridehailing.repository.UserRepository;

@Service
public class UserService {

	private final UserRepository userRepository;

	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Transactional
	public UserResponse register(CreateUserRequest request) {
		User user = new User(request.name(), request.phone());
		return UserResponse.from(userRepository.save(user));
	}

	public UserResponse getById(Long id) {
		return userRepository.findById(id)
				.map(UserResponse::from)
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
	}

}
