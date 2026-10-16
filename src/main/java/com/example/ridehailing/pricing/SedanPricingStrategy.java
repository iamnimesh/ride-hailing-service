package com.example.ridehailing.pricing;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.example.ridehailing.entity.CarType;

@Component
public class SedanPricingStrategy implements PricingStrategy {

	private final TieredFareCalculator calculator = new TieredFareCalculator(
			new BigDecimal("70.00"),
			new BigDecimal("2"), new BigDecimal("15"),
			new BigDecimal("3"), new BigDecimal("12"),
			new BigDecimal("8"));

	@Override
	public CarType carType() {
		return CarType.SEDAN;
	}

	@Override
	public BigDecimal calculateFare(BigDecimal distanceKm) {
		return calculator.calculateFare(distanceKm);
	}

}
