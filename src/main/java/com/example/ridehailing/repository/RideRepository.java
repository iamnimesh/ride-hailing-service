package com.example.ridehailing.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.ridehailing.entity.Ride;

public interface RideRepository extends JpaRepository<Ride, Long> {

	List<Ride> findByUser_IdOrderByIdDesc(Long userId);

	List<Ride> findByDriver_IdOrderByIdDesc(Long driverId);

}
