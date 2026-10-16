package com.example.ridehailing.service;

import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ridehailing.dto.CouponResponse;
import com.example.ridehailing.dto.CreateCouponRequest;
import com.example.ridehailing.entity.Coupon;
import com.example.ridehailing.exception.InvalidCouponException;
import com.example.ridehailing.exception.ResourceNotFoundException;
import com.example.ridehailing.repository.CouponRepository;

@Service
public class CouponService {

	private final CouponRepository couponRepository;

	public CouponService(CouponRepository couponRepository) {
		this.couponRepository = couponRepository;
	}

	@Transactional
	public CouponResponse create(CreateCouponRequest request) {
		String code = normalize(request.code());
		if (couponRepository.existsByCode(code)) {
			throw new InvalidCouponException("Coupon already exists: " + code);
		}
		Coupon coupon = new Coupon(code, request.discountPercentage());
		return CouponResponse.from(couponRepository.save(coupon));
	}

	@Transactional
	public void delete(String code) {
		String normalized = normalize(code);
		Coupon coupon = couponRepository.findByCode(normalized)
				.orElseThrow(() -> new ResourceNotFoundException("Coupon not found: " + normalized));
		couponRepository.delete(coupon);
	}

	public Coupon findForBooking(String couponCode) {
		if (couponCode == null || couponCode.isBlank()) {
			return null;
		}
		String code = normalize(couponCode);
		return couponRepository.findByCode(code)
				.orElseThrow(() -> new InvalidCouponException("Invalid coupon: " + code));
	}

	private String normalize(String code) {
		return code.trim().toUpperCase(Locale.ROOT);
	}

}
