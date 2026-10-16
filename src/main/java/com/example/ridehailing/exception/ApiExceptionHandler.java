package com.example.ridehailing.exception;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.ridehailing.dto.ErrorResponse;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ErrorResponse> notFound(ResourceNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(exception.getMessage()));
	}

	@ExceptionHandler(NoEligibleDriverException.class)
	public ResponseEntity<ErrorResponse> noEligibleDriver(NoEligibleDriverException exception) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(exception.getMessage()));
	}

	@ExceptionHandler(RideAlreadyCompletedException.class)
	public ResponseEntity<ErrorResponse> alreadyCompleted(RideAlreadyCompletedException exception) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(exception.getMessage()));
	}

	@ExceptionHandler(InvalidDistanceException.class)
	public ResponseEntity<ErrorResponse> invalidDistance(InvalidDistanceException exception) {
		return ResponseEntity.badRequest().body(new ErrorResponse(exception.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> invalid(MethodArgumentNotValidException exception) {
		String message = exception.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.collect(Collectors.joining("; "));
		if (message.isBlank()) {
			message = "Invalid request";
		}
		return ResponseEntity.badRequest().body(new ErrorResponse(message));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> unreadable(HttpMessageNotReadableException exception) {
		return ResponseEntity.badRequest().body(new ErrorResponse("Invalid request body"));
	}

}
