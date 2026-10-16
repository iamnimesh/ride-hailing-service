package com.example.ridehailing.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.ridehailing.entity.Driver;

public interface DriverRepository extends JpaRepository<Driver, Long> {

	List<Driver> findByAvailableTrue();

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("UPDATE Driver d SET d.available = false WHERE d.id = :driverId AND d.available = true")
	int claimIfAvailable(@Param("driverId") Long driverId);

}
