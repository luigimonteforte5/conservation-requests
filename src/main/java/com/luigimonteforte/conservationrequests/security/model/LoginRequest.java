package com.luigimonteforte.conservationrequests.security.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
		@Schema(description = "The account name to authenticate", example = "admin") @NotBlank String username,

		@Schema(description = "The account password", example = "change-me", format = "password") @NotBlank String password) {
}
