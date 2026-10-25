package com.nugget.fuel.repo;

import com.nugget.fuel.model.Station;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StationRepository extends JpaRepository<Station, Long> {}
