package com.example.ridehailing.service;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.ridehailing.entity.CarType;
import com.example.ridehailing.pricing.PricingStrategy;

@Service
public class PricingService {

	private final Map<CarType, PricingStrategy> strategies;

	public PricingService(List<PricingStrategy> strategies) {
		Map<CarType, PricingStrategy> mapped = new EnumMap<>(CarType.class);
		for (PricingStrategy strategy : strategies) {
			mapped.put(strategy.carType(), strategy);
		}
		this.strategies = Map.copyOf(mapped);
	}

	public BigDecimal calculateFare(CarType carType, BigDecimal distanceKm) {
		PricingStrategy strategy = strategies.get(carType);
		if (strategy == null) {
			throw new IllegalStateException("No pricing strategy for " + carType);
		}
		return strategy.calculateFare(distanceKm);
	}

}
