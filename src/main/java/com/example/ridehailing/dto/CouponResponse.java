package com.example.ridehailing.dto;

import java.math.BigDecimal;

import com.example.ridehailing.entity.Coupon;

public record CouponResponse(Long id, String code, BigDecimal discountPercentage) {

	public static CouponResponse from(Coupon coupon) {
		return new CouponResponse(coupon.getId(), coupon.getCode(), coupon.getDiscountPercentage());
	}

}
