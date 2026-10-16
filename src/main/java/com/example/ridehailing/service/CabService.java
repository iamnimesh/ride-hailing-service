package com.example.ridehailing.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ridehailing.dto.CabResponse;
import com.example.ridehailing.dto.UpdateCabLocationRequest;
import com.example.ridehailing.entity.Cab;
import com.example.ridehailing.exception.ResourceNotFoundException;
import com.example.ridehailing.repository.CabRepository;

@Service
public class CabService {

	private final CabRepository cabRepository;

	public CabService(CabRepository cabRepository) {
		this.cabRepository = cabRepository;
	}

	@Transactional
	public CabResponse updateLocation(Long id, UpdateCabLocationRequest request) {
		Cab cab = cabRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Cab not found: " + id));
		cab.updateLocation(request.latitude(), request.longitude());
		return CabResponse.from(cab);
	}

}
