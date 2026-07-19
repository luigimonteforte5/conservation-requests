package com.luigimonteforte.conservationrequests.model;

import com.luigimonteforte.conservationrequests.entity.Request;
import com.luigimonteforte.conservationrequests.entity.Status;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * DTO for {@link Request}
 */
public record RequestDto(
        @Schema(description = "Identifier assigned by this service", example = "42") Long id,
        @Schema(description = "Identifier of the request in the producer's own system", example = "10001") Long externalId,
        @Schema(description = "Identifier of the producer that sent the request", example = "7") Long producerId,
        @Schema(description = "Kind of document being conserved", example = "INVOICE") String documentType,
        @Schema(description = "Where the request currently sits in its lifecycle") Status status,
        @Schema(description = "When the request was first received", example = "2026-07-19T09:15:00Z") Instant createdAt,
        @Schema(description = "When the request last changed", example = "2026-07-19T09:20:00Z") Instant updatedAt,
        @Schema(description = "The documents attached to the request") List<DocumentDto> documents) {
}
