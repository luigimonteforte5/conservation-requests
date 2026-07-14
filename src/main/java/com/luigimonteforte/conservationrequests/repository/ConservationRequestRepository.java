package com.luigimonteforte.conservationrequests.repository;

import com.luigimonteforte.conservationrequests.entity.ConservationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConservationRequestRepository extends JpaRepository<ConservationRequest,Long> {
}
