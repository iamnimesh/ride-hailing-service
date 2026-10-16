package com.example.ridehailing.dto;

import com.example.ridehailing.entity.Driver;

public record DriverResponse(
		Long id,
		String name,
		String phone,
		boolean available,
		CabResponse cab
) {

	public static DriverResponse from(Driver driver) {
		return new DriverResponse(
				driver.getId(),
				driver.getName(),
				driver.getPhone(),
				driver.isAvailable(),
				CabResponse.from(driver.getCab()));
	}

}
