package com.nugget.fuel.dto;

import jakarta.validation.constraints.*;

/**
 * Self-registration. role = ATTENDANT (default), SUPERVISOR or MANAGER.
 * Attendants and supervisors must choose a branch; managers see every branch, so they do not.
 * Supervisors and managers must also give the access code the company gave them.
 * The phone number becomes the username.
 */
public record RegisterRequest(
        @NotBlank String firstName,
        @NotBlank String surname,
        @NotBlank String phone,
        String role,
        Long branchId,
        String accessCode,
        @NotBlank @Size(min = 6, max = 100) String password) {}
