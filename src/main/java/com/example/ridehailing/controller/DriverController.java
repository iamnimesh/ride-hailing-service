package com.example.ridehailing.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ridehailing.dto.CreateDriverRequest;
import com.example.ridehailing.dto.DriverResponse;
import com.example.ridehailing.service.DriverService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/drivers")
public class DriverController {

	private final DriverService driverService;

	public DriverController(DriverService driverService) {
		this.driverService = driverService;
	}

	@PostMapping
	public ResponseEntity<DriverResponse> register(@Valid @RequestBody CreateDriverRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(driverService.register(request));
	}

	@GetMapping("/{id}")
	public DriverResponse get(@PathVariable Long id) {
		return driverService.getById(id);
	}

}
