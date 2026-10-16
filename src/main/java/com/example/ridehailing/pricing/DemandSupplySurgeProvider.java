package com.example.ridehailing.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.ridehailing.service.DistanceCalculator;

/**
 * In-memory demand tracker for one application instance.
 * Each quote records the current request, drops entries older than the window,
 * and derives the multiplier from that demand/supply pair. The stored multiplier
 * is not revised when the ride ends.
 */
@Component
public class DemandSupplySurgeProvider implements SurgeMultiplierProvider {

	private static final BigDecimal NORMAL_MULTIPLIER = new BigDecimal("1.00");

	private final double radiusKm;
	private final long windowMillis;
	private final BigDecimal mediumRatio;
	private final BigDecimal highRatio;
	private final BigDecimal mediumMultiplier;
	private final BigDecimal highMultiplier;
	private final Clock clock;
	private final Deque<DemandPoint> requests = new ArrayDeque<>();

	@Autowired
	public DemandSupplySurgeProvider(@Value("${ride.matching.radius-km:5}") double radiusKm,
			@Value("${ride.surge.window-minutes:5}") long windowMinutes,
			@Value("${ride.surge.medium-ratio:2}") BigDecimal mediumRatio,
			@Value("${ride.surge.high-ratio:4}") BigDecimal highRatio,
			@Value("${ride.surge.medium-multiplier:1.25}") BigDecimal mediumMultiplier,
			@Value("${ride.surge.high-multiplier:1.5}") BigDecimal highMultiplier) {
		this(radiusKm, Duration.ofMinutes(windowMinutes), mediumRatio, highRatio, mediumMultiplier, highMultiplier,
				Clock.systemUTC());
	}

	DemandSupplySurgeProvider(double radiusKm, Duration window, BigDecimal mediumRatio, BigDecimal highRatio,
			BigDecimal mediumMultiplier, BigDecimal highMultiplier, Clock clock) {
		this.radiusKm = radiusKm;
		this.windowMillis = window.toMillis();
		this.mediumRatio = mediumRatio;
		this.highRatio = highRatio;
		this.mediumMultiplier = mediumMultiplier.setScale(2, RoundingMode.HALF_UP);
		this.highMultiplier = highMultiplier.setScale(2, RoundingMode.HALF_UP);
		this.clock = clock;
	}

	@Override
	public SurgeSnapshot quote(double latitude, double longitude, int nearbyAvailableDrivers) {
		int supply = Math.max(nearbyAvailableDrivers, 0);
		int demand = recordAndCount(latitude, longitude);
		return new SurgeSnapshot(demand, supply, multiplierFor(demand, supply));
	}

	private int recordAndCount(double latitude, double longitude) {
		long now = clock.millis();
		long cutoff = now - windowMillis;
		synchronized (requests) {
			while (!requests.isEmpty() && requests.peekFirst().requestedAtMillis() < cutoff) {
				requests.removeFirst();
			}
			requests.addLast(new DemandPoint(latitude, longitude, now));
			int demand = 0;
			for (DemandPoint point : requests) {
				double distanceKm = DistanceCalculator.haversineKm(latitude, longitude, point.latitude(),
						point.longitude());
				if (distanceKm <= radiusKm) {
					demand++;
				}
			}
			return demand;
		}
	}

	private BigDecimal multiplierFor(int demand, int supply) {
		if (supply == 0) {
			return demand > 0 ? highMultiplier : NORMAL_MULTIPLIER;
		}
		BigDecimal ratio = BigDecimal.valueOf(demand).divide(BigDecimal.valueOf(supply), 4, RoundingMode.HALF_UP);
		if (ratio.compareTo(highRatio) >= 0) {
			return highMultiplier;
		}
		if (ratio.compareTo(mediumRatio) >= 0) {
			return mediumMultiplier;
		}
		return NORMAL_MULTIPLIER;
	}

	private record DemandPoint(double latitude, double longitude, long requestedAtMillis) {
	}

}
