package com.luigimonteforte.conservationrequests.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateRequestDto(
		@NotNull @Schema(description = "Identifier of the producer sending the request", example = "7") Long producerId,
		@NotBlank @Schema(description = "Kind of document being conserved", example = "INVOICE") String documentType,
		@NotNull @Schema(description = "Identifier of the request in the producer's own system; "
				+ "must be unique for that producer", example = "10001") Long externalId,
		@NotNull @NotEmpty @Schema(description = "The documents attached to the request; at least one is required")
		List<@Valid DocumentDto> documents) {
}
