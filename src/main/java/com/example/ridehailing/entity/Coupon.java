package com.example.ridehailing.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "coupons")
public class Coupon {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String code;

	@Column(nullable = false, precision = 5, scale = 2)
	private BigDecimal discountPercentage;

	protected Coupon() {
	}

	public Coupon(String code, BigDecimal discountPercentage) {
		this.code = code;
		this.discountPercentage = discountPercentage;
	}

	public Long getId() {
		return id;
	}

	public String getCode() {
		return code;
	}

	public BigDecimal getDiscountPercentage() {
		return discountPercentage;
	}

}
