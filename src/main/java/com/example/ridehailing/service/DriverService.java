package com.example.ridehailing.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ridehailing.dto.CreateDriverRequest;
import com.example.ridehailing.dto.DriverResponse;
import com.example.ridehailing.entity.Cab;
import com.example.ridehailing.entity.Driver;
import com.example.ridehailing.exception.ResourceNotFoundException;
import com.example.ridehailing.repository.DriverRepository;

@Service
public class DriverService {

	private final DriverRepository driverRepository;

	public DriverService(DriverRepository driverRepository) {
		this.driverRepository = driverRepository;
	}

	@Transactional
	public DriverResponse register(CreateDriverRequest request) {
		Driver driver = new Driver(request.name(), request.phone());
		Cab cab = new Cab(driver, request.carType(), request.latitude(), request.longitude());
		driver.setCab(cab);
		return DriverResponse.from(driverRepository.save(driver));
	}

	public DriverResponse getById(Long id) {
		return driverRepository.findById(id)
				.map(DriverResponse::from)
				.orElseThrow(() -> new ResourceNotFoundException("Driver not found: " + id));
	}

}
