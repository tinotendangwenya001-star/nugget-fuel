package com.nugget.fuel.repo;

import com.nugget.fuel.model.FuelDiscount;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FuelDiscountRepository extends JpaRepository<FuelDiscount, Long> {
    Optional<FuelDiscount> findByFuelTypeAndEffectiveDate(String fuelType, LocalDate date);
    /** The discount in force on a date = the latest one that started on or before it. */
    Optional<FuelDiscount> findFirstByFuelTypeAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(String fuelType, LocalDate date);
    Optional<FuelDiscount> findFirstByFuelTypeAndEffectiveDateGreaterThanOrderByEffectiveDateAsc(String fuelType, LocalDate date);
}
