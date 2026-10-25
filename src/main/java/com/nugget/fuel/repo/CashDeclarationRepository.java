package com.nugget.fuel.repo;

import com.nugget.fuel.model.CashDeclaration;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CashDeclarationRepository extends JpaRepository<CashDeclaration, Long> {
    List<CashDeclaration> findByAttendantIdAndDeclDate(Long attendantId, LocalDate date);
    void deleteByAttendantIdAndDeclDate(Long attendantId, LocalDate date);
    List<CashDeclaration> findByDeclDateAndAttendantStationId(LocalDate date, Long stationId);
}
