package com.luigimonteforte.conservationrequests.utils;

import com.luigimonteforte.conservationrequests.entity.Request;
import com.luigimonteforte.conservationrequests.entity.Status;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

@UtilityClass
public class RequestSpecification {
    public static Specification<Request> hasProducerId(Long producerId) {
        return (root, _, cb) -> producerId == null ? null : cb.equal(root.get("producerId"), producerId);
    }

    public static Specification<Request> hasStatus(Status status) {
        return (root, _, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }
}
