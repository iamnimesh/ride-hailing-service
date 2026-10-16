package com.example.ridehailing;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.example.ridehailing.pricing.HatchbackPricingStrategy;
import com.example.ridehailing.pricing.PricingStrategy;
import com.example.ridehailing.pricing.SedanPricingStrategy;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TieredPricingTest {

	private final PricingStrategy hatchback = new HatchbackPricingStrategy();
	private final PricingStrategy sedan = new SedanPricingStrategy();

	@Test
	void shortRideUsesMinimumFare() {
		assertEquals(new BigDecimal("50.00"), hatchback.calculateFare(new BigDecimal("1")));
		assertEquals(new BigDecimal("50.00"), hatchback.calculateFare(new BigDecimal("5")));
		assertEquals(new BigDecimal("70.00"), sedan.calculateFare(new BigDecimal("1")));
		assertEquals(new BigDecimal("70.00"), sedan.calculateFare(new BigDecimal("5")));
	}

	@Test
	void hatchbackSevenKmUsesIncrementalSlabs() {
		assertEquals(new BigDecimal("54.00"), hatchback.calculateFare(new BigDecimal("7")));
	}

	@Test
	void sedanSevenKmUsesHigherSlabRates() {
		assertEquals(new BigDecimal("82.00"), sedan.calculateFare(new BigDecimal("7")));
	}

	@Test
	void roundsFareHalfUpToTwoDecimalPlaces() {
		assertEquals(new BigDecimal("54.07"), hatchback.calculateFare(new BigDecimal("7.013")));
	}

}
