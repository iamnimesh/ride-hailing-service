package com.example.ridehailing.dto;

import java.math.BigDecimal;

import com.example.ridehailing.entity.CarType;
import com.example.ridehailing.entity.Ride;
import com.example.ridehailing.entity.RideStatus;

public record RideHistoryResponse(
		Long rideId,
		Long userId,
		Long driverId,
		CarType requestedCarType,
		CarType assignedCarType,
		RideStatus status,
		double pickupLatitude,
		double pickupLongitude,
		Double distanceKm,
		BigDecimal finalFare
) {

	public static RideHistoryResponse from(Ride ride) {
		return new RideHistoryResponse(
				ride.getId(),
				ride.getUser().getId(),
				ride.getDriver().getId(),
				ride.getRequestedCarType(),
				ride.getAssignedCarType(),
				ride.getStatus(),
				ride.getPickupLatitude(),
				ride.getPickupLongitude(),
				ride.getDistanceKm(),
				ride.getFinalFare());
	}

}
