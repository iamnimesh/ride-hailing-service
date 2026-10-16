package com.example.ridehailing.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ridehailing.dto.BookRideRequest;
import com.example.ridehailing.dto.EndRideRequest;
import com.example.ridehailing.dto.EndRideResponse;
import com.example.ridehailing.dto.RideBookingResponse;
import com.example.ridehailing.dto.RideHistoryResponse;
import com.example.ridehailing.entity.Cab;
import com.example.ridehailing.entity.CarType;
import com.example.ridehailing.entity.Coupon;
import com.example.ridehailing.entity.Driver;
import com.example.ridehailing.entity.Ride;
import com.example.ridehailing.entity.RideStatus;
import com.example.ridehailing.entity.User;
import com.example.ridehailing.exception.InvalidDistanceException;
import com.example.ridehailing.exception.NoEligibleDriverException;
import com.example.ridehailing.exception.ResourceNotFoundException;
import com.example.ridehailing.exception.RideAlreadyCompletedException;
import com.example.ridehailing.repository.DriverRepository;
import com.example.ridehailing.repository.RideRepository;
import com.example.ridehailing.repository.UserRepository;

@Service
public class RideService {

	private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

	private final UserRepository userRepository;
	private final DriverRepository driverRepository;
	private final RideRepository rideRepository;
	private final PricingService pricingService;
	private final CouponService couponService;
	private final double searchRadiusKm;

	public RideService(UserRepository userRepository, DriverRepository driverRepository, RideRepository rideRepository,
			PricingService pricingService, CouponService couponService,
			@Value("${ride.matching.radius-km:5}") double searchRadiusKm) {
		this.userRepository = userRepository;
		this.driverRepository = driverRepository;
		this.rideRepository = rideRepository;
		this.pricingService = pricingService;
		this.couponService = couponService;
		this.searchRadiusKm = searchRadiusKm;
	}

	@Transactional
	public RideBookingResponse book(BookRideRequest request) {
		Coupon coupon = couponService.findForBooking(request.couponCode());
		User user = userRepository.findById(request.userId())
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.userId()));

		Driver driver = selectDriver(request.pickupLatitude(), request.pickupLongitude(), request.carType());
		Cab cab = driver.getCab();
		driver.markUnavailable();

		CarType requestedCarType = request.carType();
		Ride ride = new Ride(user, driver, cab, requestedCarType, cab.getCarType(), requestedCarType,
				request.pickupLatitude(), request.pickupLongitude());
		if (coupon != null) {
			ride.applyCoupon(coupon.getCode(), coupon.getDiscountPercentage());
		}
		return RideBookingResponse.from(rideRepository.save(ride));
	}

	@Transactional
	public EndRideResponse end(Long rideId, EndRideRequest request) {
		validateDistance(request.distanceKm());
		Ride ride = rideRepository.findById(rideId)
				.orElseThrow(() -> new ResourceNotFoundException("Ride not found: " + rideId));
		if (ride.getStatus() != RideStatus.ONGOING) {
			throw new RideAlreadyCompletedException("Ride already completed: " + rideId);
		}

		BigDecimal originalFare = pricingService.calculateFare(ride.getPricingCarType(),
				BigDecimal.valueOf(request.distanceKm()));
		DiscountedFare discountedFare = applyDiscount(originalFare, ride.getDiscountPercentage());
		ride.complete(request.distanceKm(), discountedFare.finalFare());
		ride.getDriver().markAvailable();
		return EndRideResponse.from(ride, discountedFare.originalFare(), discountedFare.discountAmount());
	}

	private DiscountedFare applyDiscount(BigDecimal originalFare, BigDecimal discountPercentage) {
		if (discountPercentage == null) {
			return new DiscountedFare(originalFare, BigDecimal.ZERO.setScale(2), originalFare);
		}
		BigDecimal retained = BigDecimal.ONE.subtract(discountPercentage.divide(ONE_HUNDRED, 8, RoundingMode.HALF_UP));
		BigDecimal finalFare = originalFare.multiply(retained).setScale(2, RoundingMode.HALF_UP);
		if (finalFare.signum() < 0) {
			finalFare = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}
		BigDecimal discountAmount = originalFare.subtract(finalFare).setScale(2, RoundingMode.HALF_UP);
		return new DiscountedFare(originalFare, discountAmount, finalFare);
	}

	@Transactional(readOnly = true)
	public List<RideHistoryResponse> historyForUser(Long userId) {
		if (!userRepository.existsById(userId)) {
			throw new ResourceNotFoundException("User not found: " + userId);
		}
		return rideRepository.findByUser_IdOrderByIdDesc(userId).stream()
				.map(RideHistoryResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<RideHistoryResponse> historyForDriver(Long driverId) {
		if (!driverRepository.existsById(driverId)) {
			throw new ResourceNotFoundException("Driver not found: " + driverId);
		}
		return rideRepository.findByDriver_IdOrderByIdDesc(driverId).stream()
				.map(RideHistoryResponse::from)
				.toList();
	}

	private void validateDistance(Double distanceKm) {
		if (distanceKm == null || !Double.isFinite(distanceKm) || distanceKm < 0) {
			throw new InvalidDistanceException("Distance must be a non-negative finite number");
		}
	}

	private Driver selectDriver(double pickupLatitude, double pickupLongitude, CarType requestedCarType) {
		List<NearbyDriver> nearby = new ArrayList<>();
		for (Driver driver : driverRepository.findByAvailableTrue()) {
			Cab cab = driver.getCab();
			if (cab == null) {
				continue;
			}
			double distanceKm = DistanceCalculator.haversineKm(pickupLatitude, pickupLongitude, cab.getLatitude(),
					cab.getLongitude());
			if (distanceKm <= searchRadiusKm) {
				nearby.add(new NearbyDriver(driver, distanceKm));
			}
		}

		Driver matched = nearest(nearby, requestedCarType);
		if (matched != null) {
			return matched;
		}
		if (requestedCarType == CarType.HATCHBACK) {
			Driver upgrade = nearest(nearby, CarType.SEDAN);
			if (upgrade != null) {
				return upgrade;
			}
		}
		throw new NoEligibleDriverException("No eligible driver found");
	}

	private Driver nearest(List<NearbyDriver> nearby, CarType carType) {
		NearbyDriver best = null;
		for (NearbyDriver candidate : nearby) {
			if (candidate.driver().getCab().getCarType() != carType) {
				continue;
			}
			if (best == null || isCloser(candidate, best)) {
				best = candidate;
			}
		}
		return best == null ? null : best.driver();
	}

	private boolean isCloser(NearbyDriver candidate, NearbyDriver current) {
		int byDistance = Double.compare(candidate.distanceKm(), current.distanceKm());
		if (byDistance != 0) {
			return byDistance < 0;
		}
		return candidate.driver().getId() < current.driver().getId();
	}

	private record NearbyDriver(Driver driver, double distanceKm) {
	}

	private record DiscountedFare(BigDecimal originalFare, BigDecimal discountAmount, BigDecimal finalFare) {
	}

}
