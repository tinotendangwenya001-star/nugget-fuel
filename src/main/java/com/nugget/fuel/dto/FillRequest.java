package com.nugget.fuel.dto;

import com.nugget.fuel.model.CreditFill;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record FillRequest(
        @NotNull Long pumpId,
        @NotNull CreditFill.Type type,
        String companyName,            // required for creditors
        @NotBlank String driverName,
        @NotBlank String vehicleReg,
        @NotNull @Positive BigDecimal litres) {}
