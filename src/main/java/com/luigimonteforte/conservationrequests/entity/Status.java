package com.luigimonteforte.conservationrequests.entity;

import java.util.Map;
import java.util.Set;

public enum Status {
    RECEIVED, VALIDATED, REJECTED, COMPLETED;

    private static final Map<Status, Set<Status>> ALLOWED_TRANSITIONS = Map.of(
            RECEIVED, Set.of(VALIDATED, REJECTED),
            VALIDATED, Set.of(COMPLETED),
            REJECTED, Set.of(),
            COMPLETED, Set.of()
    );

    public boolean canTransitionTo(Status target) {
        return ALLOWED_TRANSITIONS.get(this).contains(target);
    }
}
