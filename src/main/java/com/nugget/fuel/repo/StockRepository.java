package com.nugget.fuel.repo;

import com.nugget.fuel.model.StockEntry;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockRepository extends JpaRepository<StockEntry, Long> {
    Optional<StockEntry> findByStationIdAndFuelTypeAndStockDate(Long stationId, String fuelType, LocalDate date);
    /** The first day of stock records for a fuel type at a branch. */
    Optional<StockEntry> findFirstByStationIdAndFuelTypeAndBaselineTrueOrderByStockDateAsc(Long stationId, String fuelType);
    /** Deliveries / returns recorded after the first day (both dates included). */
    List<StockEntry> findByStationIdAndFuelTypeAndBaselineFalseAndStockDateBetween(
            Long stationId, String fuelType, LocalDate from, LocalDate to);
}
