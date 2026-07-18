package com.luigimonteforte.conservationrequests.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateRequestDto(@NotNull Long producerId, @NotBlank String documentType, @NotNull Long externalId,
		@NotNull @NotEmpty List<@Valid DocumentDto> documents) {
}
