package com.example.ridehailing.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.ridehailing.entity.Cab;

public interface CabRepository extends JpaRepository<Cab, Long> {
}
