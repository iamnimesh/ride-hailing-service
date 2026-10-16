package com.example.ridehailing.pricing;

import java.math.BigDecimal;

public record SurgeSnapshot(int demand, int supply, BigDecimal multiplier) {
}
