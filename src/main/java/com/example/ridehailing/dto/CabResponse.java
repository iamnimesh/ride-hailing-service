package com.example.ridehailing.dto;

import com.example.ridehailing.entity.Cab;
import com.example.ridehailing.entity.CarType;

public record CabResponse(
		Long id,
		Long driverId,
		CarType carType,
		double latitude,
		double longitude
) {

	public static CabResponse from(Cab cab) {
		return new CabResponse(
				cab.getId(),
				cab.getDriver().getId(),
				cab.getCarType(),
				cab.getLatitude(),
				cab.getLongitude());
	}

}
