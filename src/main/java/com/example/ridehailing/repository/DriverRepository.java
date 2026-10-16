package com.example.ridehailing.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.ridehailing.entity.Driver;

public interface DriverRepository extends JpaRepository<Driver, Long> {
}
