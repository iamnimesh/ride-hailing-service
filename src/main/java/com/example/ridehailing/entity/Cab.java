package com.example.ridehailing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "cabs")
public class Cab {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(optional = false)
	@JoinColumn(name = "driver_id", nullable = false, unique = true)
	private Driver driver;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private CarType carType;

	@Column(nullable = false)
	private double latitude;

	@Column(nullable = false)
	private double longitude;

	protected Cab() {
	}

	public Cab(Driver driver, CarType carType, double latitude, double longitude) {
		this.driver = driver;
		this.carType = carType;
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public void updateLocation(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public Long getId() {
		return id;
	}

	public Driver getDriver() {
		return driver;
	}

	public CarType getCarType() {
		return carType;
	}

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

}
