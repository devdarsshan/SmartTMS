package com.smartlogistics.analytics.repo;

import com.smartlogistics.analytics.entity.OrderAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderAnalyticsRepository extends JpaRepository<OrderAnalytics, Long> {
    long countByInTransitAtNotNullAndDeliveredAtIsNull();

    long countByDeliveredAtBetween(LocalDateTime startInclusive, LocalDateTime endExclusive);

    @Query("select avg(o.assignmentDurationSeconds) from OrderAnalytics o where o.assignmentDurationSeconds is not null")
    Double averageAssignmentDurationSeconds();

    @Query("select avg(o.deliveryDurationSeconds) from OrderAnalytics o where o.deliveryDurationSeconds is not null")
    Double averageDeliveryDurationSeconds();

    @Query("""
            select o.fromCity as fromCity, o.toCity as toCity, count(o) as orderCount
            from OrderAnalytics o
            where o.fromCity is not null and o.toCity is not null
            group by o.fromCity, o.toCity
            order by count(o) desc, o.fromCity asc, o.toCity asc
            """)
    List<RouteVolumeView> fetchRouteVolumes();
}
