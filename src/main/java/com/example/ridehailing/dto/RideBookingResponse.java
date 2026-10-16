package com.example.ridehailing.dto;

import com.example.ridehailing.entity.CarType;
import com.example.ridehailing.entity.Ride;
import com.example.ridehailing.entity.RideStatus;

public record RideBookingResponse(
		Long rideId,
		Long driverId,
		Long cabId,
		CarType requestedCarType,
		CarType assignedCarType,
		CarType pricingCarType,
		RideStatus status
) {

	public static RideBookingResponse from(Ride ride) {
		return new RideBookingResponse(
				ride.getId(),
				ride.getDriver().getId(),
				ride.getCab().getId(),
				ride.getRequestedCarType(),
				ride.getAssignedCarType(),
				ride.getPricingCarType(),
				ride.getStatus());
	}

}
