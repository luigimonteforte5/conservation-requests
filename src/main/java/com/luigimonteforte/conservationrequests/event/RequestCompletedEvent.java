package com.luigimonteforte.conservationrequests.event;

import com.luigimonteforte.conservationrequests.entity.Status;
import lombok.Builder;

@Builder
public record RequestCompletedEvent(Long requestId, Long producerId, Long externalId, Status previousStatus) {
}
