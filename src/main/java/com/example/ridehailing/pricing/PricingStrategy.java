package com.example.ridehailing.pricing;

import java.math.BigDecimal;

import com.example.ridehailing.entity.CarType;

public interface PricingStrategy {

	CarType carType();

	BigDecimal calculateFare(BigDecimal distanceKm);

}
