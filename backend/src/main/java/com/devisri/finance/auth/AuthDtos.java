package com.devisri.finance.auth;

import java.time.Instant;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The shapes of JSON that go in and out of the auth endpoints. */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank(message = "Name is required") @Size(max = 60, message = "Name is too long") String name,
            @Size(max = 60, message = "Occupation is too long") String occupation,
            @NotBlank(message = "Email is required") @Email(message = "Email is not valid") String email,
            @NotBlank(message = "Password is required")
            @Size(min = 6, max = 100, message = "Password must be at least 6 characters") String password) {
    }

    public record LoginRequest(
            @NotBlank(message = "Email and password required") String email,
            @NotBlank(message = "Email and password required") String password) {
    }

    public record TokenResponse(String token) {
    }

    public record ProfileResponse(String id, String name, String occupation, String email, boolean demo,
                                  Instant createdAt) {
        static ProfileResponse from(User user) {
            return new ProfileResponse(user.getId(), user.getName(), user.getOccupation(), user.getEmail(),
                    user.isDemo(), user.getCreatedAt());
        }
    }
}
