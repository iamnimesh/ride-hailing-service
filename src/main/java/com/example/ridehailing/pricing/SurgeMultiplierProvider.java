package com.example.ridehailing.pricing;

public interface SurgeMultiplierProvider {

	SurgeSnapshot quote(double latitude, double longitude, int nearbyAvailableDrivers);

}
