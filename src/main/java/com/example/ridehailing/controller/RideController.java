package com.example.ridehailing.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ridehailing.dto.BookRideRequest;
import com.example.ridehailing.dto.EndRideRequest;
import com.example.ridehailing.dto.EndRideResponse;
import com.example.ridehailing.dto.RideBookingResponse;
import com.example.ridehailing.service.RideService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/rides")
public class RideController {

	private final RideService rideService;

	public RideController(RideService rideService) {
		this.rideService = rideService;
	}

	@PostMapping("/book")
	public ResponseEntity<RideBookingResponse> book(@Valid @RequestBody BookRideRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(rideService.book(request));
	}

	@PostMapping("/{rideId}/end")
	public EndRideResponse end(@PathVariable Long rideId, @Valid @RequestBody EndRideRequest request) {
		return rideService.end(rideId, request);
	}

}
