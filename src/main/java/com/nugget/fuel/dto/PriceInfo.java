package com.nugget.fuel.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Current price of a fuel type, plus the next scheduled price (if any). */
public record PriceInfo(String fuelType, BigDecimal pricePerLitre, LocalDate since,
                        BigDecimal nextPrice, LocalDate nextDate) {}
