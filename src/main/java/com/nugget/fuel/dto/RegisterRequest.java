package com.nugget.fuel.dto;

import jakarta.validation.constraints.*;

/** Self-registration of a fuel attendant. The phone number becomes the username. */
public record RegisterRequest(
        @NotBlank String firstName,
        @NotBlank String surname,
        @NotBlank String phone,
        @NotNull Long branchId,
        @NotBlank @Size(min = 6, max = 100) String password) {}
