package com.example.ridehailing.exception;

public class RideAlreadyCompletedException extends RuntimeException {

	public RideAlreadyCompletedException(String message) {
		super(message);
	}

}
