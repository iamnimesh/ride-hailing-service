package com.example.ridehailing.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.ridehailing.entity.Ride;

public interface RideRepository extends JpaRepository<Ride, Long> {
}
