package com.example.ridehailing.service;

public final class DistanceCalculator {

	private static final double EARTH_RADIUS_KM = 6371.0;

	private DistanceCalculator() {
	}

	public static double haversineKm(double latitude1, double longitude1, double latitude2, double longitude2) {
		double latitudeDistance = Math.toRadians(latitude2 - latitude1);
		double longitudeDistance = Math.toRadians(longitude2 - longitude1);
		double startLatitude = Math.toRadians(latitude1);
		double endLatitude = Math.toRadians(latitude2);

		double a = Math.sin(latitudeDistance / 2) * Math.sin(latitudeDistance / 2)
				+ Math.cos(startLatitude) * Math.cos(endLatitude)
						* Math.sin(longitudeDistance / 2) * Math.sin(longitudeDistance / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		return EARTH_RADIUS_KM * c;
	}

}
