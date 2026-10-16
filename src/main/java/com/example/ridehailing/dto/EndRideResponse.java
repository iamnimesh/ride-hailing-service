package com.example.ridehailing.dto;

import java.math.BigDecimal;

import com.example.ridehailing.entity.Ride;
import com.example.ridehailing.entity.RideStatus;

public record EndRideResponse(Long rideId, RideStatus status, Double distanceKm, BigDecimal finalFare) {

	public static EndRideResponse from(Ride ride) {
		return new EndRideResponse(ride.getId(), ride.getStatus(), ride.getDistanceKm(), ride.getFinalFare());
	}

}
