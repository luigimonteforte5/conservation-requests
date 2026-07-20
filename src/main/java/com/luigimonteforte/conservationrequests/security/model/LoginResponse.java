package com.luigimonteforte.conservationrequests.security.model;

import io.swagger.v3.oas.annotations.media.Schema;

public record LoginResponse(
		@Schema(description = "The signed JWT to send as 'Authorization: Bearer <token>' on the other endpoints",
				example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiJ9.signature") String token) {
}
