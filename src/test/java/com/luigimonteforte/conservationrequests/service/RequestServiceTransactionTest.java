package com.luigimonteforte.conservationrequests.service;

import com.luigimonteforte.conservationrequests.entity.Request;
import com.luigimonteforte.conservationrequests.entity.Status;
import com.luigimonteforte.conservationrequests.repository.RequestRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("RequestService transaction boundary")
class RequestServiceTransactionTest {

    @Autowired
    private RequestService requestService;

    @MockitoBean
    private RequestRepository requestRepository;

    // The request read here is a mock that was never persisted, so the real history writer would fail the FK on
    // its request_id. This test is about the transaction boundary, not the audit trail, so the writer is stubbed out.
    @MockitoBean
    private RequestStatusHistoryService requestStatusHistoryService;

    @Test
    @DisplayName("changeStatus runs inside a transaction, so the row lock it takes survives until the write")
    void changeStatus_runsInsideATransaction_soTheRowLockSurvivesUntilTheWrite() {
        AtomicBoolean transactionWasActive = new AtomicBoolean();
        when(requestRepository.findWithLockById(5L)).thenAnswer(invocation -> {
            transactionWasActive.set(TransactionSynchronizationManager.isActualTransactionActive());
            return Optional.of(Request.builder().id(5L).status(Status.RECEIVED).build());
        });
        when(requestRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        requestService.changeStatus(5L, Status.VALIDATED);

        assertTrue(transactionWasActive.get(),
                "changeStatus must run in a transaction: without one the PESSIMISTIC_WRITE lock is released "
                        + "as soon as the read returns, and the concurrent transition it is meant to block gets through");
    }
}
