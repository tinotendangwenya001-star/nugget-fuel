package com.nugget.fuel.dto;

import java.math.BigDecimal;

public record PumpDto(Long id, String name, String fuelType, String station, BigDecimal pricePerLitre) {}
