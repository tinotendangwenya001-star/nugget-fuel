package com.nugget.fuel.repo;

import com.nugget.fuel.model.FuelPrice;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FuelPriceRepository extends JpaRepository<FuelPrice, Long> {
    Optional<FuelPrice> findByFuelTypeAndEffectiveDate(String fuelType, LocalDate date);
    /** The price in force on a date = the latest one that started on or before it. */
    Optional<FuelPrice> findFirstByFuelTypeAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(String fuelType, LocalDate date);
    Optional<FuelPrice> findFirstByFuelTypeOrderByEffectiveDateAsc(String fuelType);
    Optional<FuelPrice> findFirstByFuelTypeAndEffectiveDateGreaterThanOrderByEffectiveDateAsc(String fuelType, LocalDate date);
}
