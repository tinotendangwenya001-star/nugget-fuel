package com.nugget.fuel.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PriceRequest(
        @NotBlank String fuelType,
        @NotNull @Positive BigDecimal pricePerLitre,
        LocalDate effectiveDate) {}   // empty = today
