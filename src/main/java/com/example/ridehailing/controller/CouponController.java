package com.example.ridehailing.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ridehailing.dto.CouponResponse;
import com.example.ridehailing.dto.CreateCouponRequest;
import com.example.ridehailing.service.CouponService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/coupons")
public class CouponController {

	private final CouponService couponService;

	public CouponController(CouponService couponService) {
		this.couponService = couponService;
	}

	@PostMapping
	public ResponseEntity<CouponResponse> create(@Valid @RequestBody CreateCouponRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(couponService.create(request));
	}

	@DeleteMapping("/{code}")
	public ResponseEntity<Void> delete(@PathVariable String code) {
		couponService.delete(code);
		return ResponseEntity.noContent().build();
	}

}
