package com.nugget.fuel.repo;

import com.nugget.fuel.model.FuelReading;
import com.nugget.fuel.model.Pump;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReadingRepository extends JpaRepository<FuelReading, Long> {

    Optional<FuelReading> findByPumpAndReadingDate(Pump pump, LocalDate date);

    boolean existsByPump(Pump pump);

    /** Most recent reading before the given date. */
    Optional<FuelReading> findFirstByPumpAndReadingDateLessThanOrderByReadingDateDesc(Pump pump, LocalDate date);

    List<FuelReading> findByAttendantIdAndReadingDate(Long attendantId, LocalDate date);

    /** Litres sold per day for one fuel type at a branch (all pumps of that fuel added together). */
    @Query("select r.readingDate, sum(r.closingVolume - r.openingVolume) from FuelReading r " +
           "where r.pump.station.id = :sid and r.pump.fuelType = :type " +
           "and r.readingDate between :from and :to group by r.readingDate")
    List<Object[]> soldByDay(@Param("sid") Long sid, @Param("type") String type,
                             @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select r from FuelReading r where r.readingDate = :date " +
           "and (:stationId is null or r.pump.station.id = :stationId) " +
           "order by r.pump.station.name, r.pump.name")
    List<FuelReading> findForManager(@Param("date") LocalDate date, @Param("stationId") Long stationId);
}
