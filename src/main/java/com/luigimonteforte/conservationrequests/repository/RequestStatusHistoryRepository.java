package com.luigimonteforte.conservationrequests.repository;

import com.luigimonteforte.conservationrequests.entity.RequestStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RequestStatusHistoryRepository extends JpaRepository<RequestStatusHistory, Long> {
    List<RequestStatusHistory> findByRequest_IdOrderByChangedAtAscIdAsc(Long id);
}
