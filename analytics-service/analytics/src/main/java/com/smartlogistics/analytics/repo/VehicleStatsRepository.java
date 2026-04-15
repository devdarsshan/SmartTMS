package com.smartlogistics.analytics.repo;

import com.smartlogistics.analytics.entity.VehicleStats;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;

public interface VehicleStatsRepository extends JpaRepository<VehicleStats, Long> {
    long countByVehicleStatusIn(Collection<String> statuses);

    long countByLastLocationAtAfter(LocalDateTime threshold);
}
