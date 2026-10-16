package com.example.ridehailing.controller;

import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ridehailing.dto.CabResponse;
import com.example.ridehailing.dto.UpdateCabLocationRequest;
import com.example.ridehailing.service.CabService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/cabs")
public class CabController {

	private final CabService cabService;

	public CabController(CabService cabService) {
		this.cabService = cabService;
	}

	@PatchMapping("/{id}/location")
	public CabResponse updateLocation(@PathVariable Long id, @Valid @RequestBody UpdateCabLocationRequest request) {
		return cabService.updateLocation(id, request);
	}

}
