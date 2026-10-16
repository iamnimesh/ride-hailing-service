package com.example.ridehailing.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.ridehailing.entity.Driver;

public interface DriverRepository extends JpaRepository<Driver, Long> {

	List<Driver> findByAvailableTrue();

}
