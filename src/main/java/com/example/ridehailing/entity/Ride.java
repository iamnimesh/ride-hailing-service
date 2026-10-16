package com.example.ridehailing.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "rides")
public class Ride {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "driver_id", nullable = false)
	private Driver driver;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "cab_id", nullable = false)
	private Cab cab;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private CarType requestedCarType;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private CarType assignedCarType;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private CarType pricingCarType;

	@Column(nullable = false)
	private double pickupLatitude;

	@Column(nullable = false)
	private double pickupLongitude;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private RideStatus status;

	private Double distanceKm;

	@Column(precision = 12, scale = 2)
	private BigDecimal finalFare;

	private String couponCode;

	@Column(precision = 5, scale = 2)
	private BigDecimal discountPercentage;

	protected Ride() {
	}

	public Ride(User user, Driver driver, Cab cab, CarType requestedCarType, CarType assignedCarType,
			CarType pricingCarType, double pickupLatitude, double pickupLongitude) {
		this.user = user;
		this.driver = driver;
		this.cab = cab;
		this.requestedCarType = requestedCarType;
		this.assignedCarType = assignedCarType;
		this.pricingCarType = pricingCarType;
		this.pickupLatitude = pickupLatitude;
		this.pickupLongitude = pickupLongitude;
		this.status = RideStatus.ONGOING;
	}

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public Driver getDriver() {
		return driver;
	}

	public Cab getCab() {
		return cab;
	}

	public CarType getRequestedCarType() {
		return requestedCarType;
	}

	public CarType getAssignedCarType() {
		return assignedCarType;
	}

	public CarType getPricingCarType() {
		return pricingCarType;
	}

	public double getPickupLatitude() {
		return pickupLatitude;
	}

	public double getPickupLongitude() {
		return pickupLongitude;
	}

	public RideStatus getStatus() {
		return status;
	}

	public Double getDistanceKm() {
		return distanceKm;
	}

	public BigDecimal getFinalFare() {
		return finalFare;
	}

	public String getCouponCode() {
		return couponCode;
	}

	public BigDecimal getDiscountPercentage() {
		return discountPercentage;
	}

	public void applyCoupon(String couponCode, BigDecimal discountPercentage) {
		this.couponCode = couponCode;
		this.discountPercentage = discountPercentage;
	}

	public void complete(double distanceKm, BigDecimal finalFare) {
		this.distanceKm = distanceKm;
		this.finalFare = finalFare;
		this.status = RideStatus.COMPLETED;
	}

}
