package com.smartlogistics.analytics.repo;

import com.smartlogistics.analytics.entity.DriverStats;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DriverStatsRepository extends JpaRepository<DriverStats, Long> {
    Optional<DriverStats> findByCurrentVehicleId(Long currentVehicleId);

    List<DriverStats> findAllByOrderByDriverIdAsc();
}
