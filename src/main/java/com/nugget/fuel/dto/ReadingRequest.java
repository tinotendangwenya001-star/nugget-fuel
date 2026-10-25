package com.nugget.fuel.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Normal day: pumpId and the three closing readings (the fuel price is set by the manager).
 * First-time setup for a pump: also send date (the previous day) and the three opening readings.
 * lunchAmount / transportAmount are only used on the lunch pump (Diesel Pump B); transport only on Mondays.
 */
public record ReadingRequest(
        @NotNull Long pumpId,
        LocalDate date,
        @PositiveOrZero BigDecimal openingManual,
        @PositiveOrZero BigDecimal openingVolume,
        @PositiveOrZero BigDecimal openingSales,
        @NotNull @PositiveOrZero BigDecimal closingManual,
        @NotNull @PositiveOrZero BigDecimal closingVolume,
        @NotNull @PositiveOrZero BigDecimal closingSales,
        @PositiveOrZero BigDecimal lunchAmount,
        @PositiveOrZero BigDecimal transportAmount) {}
