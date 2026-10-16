package com.example.ridehailing.pricing;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.example.ridehailing.entity.CarType;

@Component
public class HatchbackPricingStrategy implements PricingStrategy {

	private final TieredFareCalculator calculator = new TieredFareCalculator(
			new BigDecimal("50.00"),
			new BigDecimal("2"), new BigDecimal("10"),
			new BigDecimal("3"), new BigDecimal("8"),
			new BigDecimal("5"));

	@Override
	public CarType carType() {
		return CarType.HATCHBACK;
	}

	@Override
	public BigDecimal calculateFare(BigDecimal distanceKm) {
		return calculator.calculateFare(distanceKm);
	}

}
