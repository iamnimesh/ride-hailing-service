package com.example.ridehailing.dto;

import java.math.BigDecimal;

import com.example.ridehailing.entity.Ride;
import com.example.ridehailing.entity.RideStatus;

public record EndRideResponse(
		Long rideId,
		RideStatus status,
		Double distanceKm,
		BigDecimal surgeMultiplier,
		BigDecimal originalFare,
		BigDecimal discountAmount,
		BigDecimal finalFare
) {

	public static EndRideResponse from(Ride ride, BigDecimal originalFare, BigDecimal discountAmount) {
		return new EndRideResponse(
				ride.getId(),
				ride.getStatus(),
				ride.getDistanceKm(),
				ride.getSurgeMultiplier(),
				originalFare,
				discountAmount,
				ride.getFinalFare());
	}

}
