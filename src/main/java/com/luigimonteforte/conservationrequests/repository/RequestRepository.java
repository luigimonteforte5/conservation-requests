package com.luigimonteforte.conservationrequests.repository;

import com.luigimonteforte.conservationrequests.entity.Request;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RequestRepository extends JpaRepository<Request,Long>, JpaSpecificationExecutor<Request> {
    boolean existsByExternalIdAndProducerId(Long externalId, Long producerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Request> findWithLockById(Long id);
}
