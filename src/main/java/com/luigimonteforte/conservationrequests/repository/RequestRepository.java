package com.luigimonteforte.conservationrequests.repository;

import com.luigimonteforte.conservationrequests.entity.Request;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface RequestRepository extends JpaRepository<Request,Long>, JpaSpecificationExecutor<Request> {
    boolean existsByExternalIdAndProducerId(Long externalId, Long producerId);
}
