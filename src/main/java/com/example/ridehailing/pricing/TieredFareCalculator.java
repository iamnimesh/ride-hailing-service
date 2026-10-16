package com.example.ridehailing.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class TieredFareCalculator {

	private final BigDecimal minimumFare;
	private final BigDecimal firstSlabKm;
	private final BigDecimal firstSlabRate;
	private final BigDecimal secondSlabKm;
	private final BigDecimal secondSlabRate;
	private final BigDecimal beyondRate;

	public TieredFareCalculator(BigDecimal minimumFare, BigDecimal firstSlabKm, BigDecimal firstSlabRate,
			BigDecimal secondSlabKm, BigDecimal secondSlabRate, BigDecimal beyondRate) {
		this.minimumFare = minimumFare;
		this.firstSlabKm = firstSlabKm;
		this.firstSlabRate = firstSlabRate;
		this.secondSlabKm = secondSlabKm;
		this.secondSlabRate = secondSlabRate;
		this.beyondRate = beyondRate;
	}

	public BigDecimal calculateFare(BigDecimal distanceKm) {
		BigDecimal remaining = distanceKm;
		BigDecimal firstDistance = remaining.min(firstSlabKm);
		remaining = remaining.subtract(firstDistance);
		BigDecimal secondDistance = remaining.min(secondSlabKm);
		remaining = remaining.subtract(secondDistance);

		BigDecimal fare = firstDistance.multiply(firstSlabRate)
				.add(secondDistance.multiply(secondSlabRate))
				.add(remaining.multiply(beyondRate));
		if (fare.compareTo(minimumFare) < 0) {
			fare = minimumFare;
		}
		return fare.setScale(2, RoundingMode.HALF_UP);
	}

}
