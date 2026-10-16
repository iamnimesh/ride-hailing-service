package com.example.ridehailing.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ridehailing.dto.CreateUserRequest;
import com.example.ridehailing.dto.RideHistoryResponse;
import com.example.ridehailing.dto.UserResponse;
import com.example.ridehailing.service.RideService;
import com.example.ridehailing.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/users")
public class UserController {

	private final UserService userService;
	private final RideService rideService;

	public UserController(UserService userService, RideService rideService) {
		this.userService = userService;
		this.rideService = rideService;
	}

	@PostMapping
	public ResponseEntity<UserResponse> register(@Valid @RequestBody CreateUserRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(userService.register(request));
	}

	@GetMapping("/{id}")
	public UserResponse get(@PathVariable Long id) {
		return userService.getById(id);
	}

	@GetMapping("/{id}/rides")
	public List<RideHistoryResponse> rides(@PathVariable Long id) {
		return rideService.historyForUser(id);
	}

}
