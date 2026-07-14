package com.luigimonteforte.conservationrequests.model;

import com.luigimonteforte.conservationrequests.entity.ConservationRequest;
import com.luigimonteforte.conservationrequests.entity.Status;

import java.time.Instant;
import java.util.List;

/**
 * DTO for {@link ConservationRequest}
 */
public record ConservationRequestDto(Long externalId, Long producerId, String documentType, Status status,
                                     Instant createdAt, Instant updatedAt, List<DocumentDto> documents) {
}