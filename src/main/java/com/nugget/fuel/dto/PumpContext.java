package com.nugget.fuel.dto;

import com.nugget.fuel.dto.ReadingResponse.Values;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PumpContext(
        boolean needsSetup,
        LocalDate suggestedSetupDate,
        LocalDate openingDate,
        Values opening,
        String fuelType,
        BigDecimal pricePerLitre,    // today's price, set by the manager
        boolean lunchAllowed,        // Diesel Pump B
        boolean transportAllowed) {} // Diesel Pump B on a Monday
