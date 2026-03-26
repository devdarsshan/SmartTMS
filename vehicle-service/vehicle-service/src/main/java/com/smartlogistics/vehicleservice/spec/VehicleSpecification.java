package com.smartlogistics.vehicleservice.spec;

import com.smartlogistics.vehicleservice.entity.Vehicle;
import com.smartlogistics.vehicleservice.enums.VehicleStatus;
import com.smartlogistics.vehicleservice.enums.VehicleType;
import org.springframework.data.jpa.domain.Specification;

public class VehicleSpecification {

    public static Specification<Vehicle> hasVehicleType(VehicleType vehicleType) {
        return (root, query, cb) -> {
            if (vehicleType == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("vehicleType"), vehicleType);
        };
    }

    public static Specification<Vehicle> hasVehicleCode(String vehicleCode) {
        return (root, query, cb) ->
                vehicleCode == null ? cb.conjunction() : cb.equal(root.get("vehicleCode"), vehicleCode);
    }

    public static Specification<Vehicle> hasFrom(String from) {
        return (root, query, cb) ->
                from == null ? cb.conjunction() : cb.equal(cb.lower(root.get("from")), from.toLowerCase());
    }

    public static Specification<Vehicle> hasVehicleStatus(VehicleStatus vehicleStatus) {
        return (root, query, cb) ->
                vehicleStatus == null ? cb.conjunction() : cb.equal(root.get("vehicleStatus"), vehicleStatus);
    }

}

