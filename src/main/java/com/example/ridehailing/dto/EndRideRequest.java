package com.example.ridehailing.dto;

import jakarta.validation.constraints.NotNull;

public record EndRideRequest(@NotNull Double distanceKm) {
}
