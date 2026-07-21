package com.luigimonteforte.conservationrequests.model;

import com.luigimonteforte.conservationrequests.entity.Status;

import java.time.Instant;

/**
 * DTO for {@link com.luigimonteforte.conservationrequests.entity.RequestStatusHistory}
 */
public record RequestStatusHistoryDto(Status fromStatus, Status toStatus, Instant changedAt) {
}