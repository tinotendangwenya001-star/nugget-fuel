package com.nugget.fuel.repo;

import com.nugget.fuel.model.CreditFill;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditFillRepository extends JpaRepository<CreditFill, Long> {
    List<CreditFill> findByReadingIdOrderById(Long readingId);
}
