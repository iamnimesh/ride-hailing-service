package com.example.ridehailing.pricing;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SurgeMultiplierTest {

	@Test
	void ratioBandsAndZeroSupply() {
		AdjustableClock clock = new AdjustableClock();
		DemandSupplySurgeProvider provider = provider(clock);

		assertEquals(new BigDecimal("1.00"), provider.quote(0, 0, 1).multiplier());
		assertEquals(new BigDecimal("1.25"), provider.quote(0, 0, 1).multiplier());
		assertEquals(new BigDecimal("1.25"), provider.quote(0, 0, 1).multiplier());
		SurgeSnapshot high = provider.quote(0, 0, 1);
		assertEquals(4, high.demand());
		assertEquals(1, high.supply());
		assertEquals(new BigDecimal("1.50"), high.multiplier());

		SurgeSnapshot noDrivers = provider.quote(20, 20, 0);
		assertEquals(1, noDrivers.demand());
		assertEquals(0, noDrivers.supply());
		assertEquals(new BigDecimal("1.50"), noDrivers.multiplier());
	}

	@Test
	void expiredRequestsAreRemoved() {
		AdjustableClock clock = new AdjustableClock();
		DemandSupplySurgeProvider provider = provider(clock);
		provider.quote(1, 1, 1);
		provider.quote(1, 1, 1);

		clock.plusMinutes(6);

		SurgeSnapshot fresh = provider.quote(1, 1, 1);
		assertEquals(1, fresh.demand());
		assertEquals(new BigDecimal("1.00"), fresh.multiplier());
	}

	private static DemandSupplySurgeProvider provider(Clock clock) {
		return new DemandSupplySurgeProvider(5, Duration.ofMinutes(5), new BigDecimal("2"), new BigDecimal("4"),
				new BigDecimal("1.25"), new BigDecimal("1.5"), clock);
	}

	private static final class AdjustableClock extends Clock {

		private long millis = 1_700_000_000_000L;

		void plusMinutes(long minutes) {
			millis += Duration.ofMinutes(minutes).toMillis();
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return Instant.ofEpochMilli(millis);
		}

	}

}
