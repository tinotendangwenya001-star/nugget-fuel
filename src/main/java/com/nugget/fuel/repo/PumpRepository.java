package com.nugget.fuel.repo;

import com.nugget.fuel.model.Pump;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PumpRepository extends JpaRepository<Pump, Long> {
    List<Pump> findByStationId(Long stationId);
}
