package com.luigimonteforte.conservationrequests.service;

import com.luigimonteforte.conservationrequests.entity.Request;
import com.luigimonteforte.conservationrequests.entity.RequestStatusHistory;
import com.luigimonteforte.conservationrequests.entity.Status;
import com.luigimonteforte.conservationrequests.model.CreateRequestDto;
import com.luigimonteforte.conservationrequests.model.DocumentDto;
import com.luigimonteforte.conservationrequests.model.RequestDto;
import com.luigimonteforte.conservationrequests.repository.RequestStatusHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Request status history — atomicity with the change that caused it")
class RequestStatusHistoryIntegrationTest {

	@Autowired
	private RequestService requestService;

	@Autowired
	private RequestStatusHistoryService requestStatusHistoryService;

	@Autowired
	private RequestStatusHistoryRepository requestStatusHistoryRepository;

	@Test
	@DisplayName("recordHistory refuses to run without an ambient transaction, so an audit row can never be written apart from the change it records")
	void recordHistory_isRejected_whenThereIsNoTransaction() {
		Request detached = Request.builder().id(1L).build();

		// No @Transactional here on purpose: MANDATORY must reject the call before it can touch the database.
		assertThrows(IllegalTransactionStateException.class,
				() -> requestStatusHistoryService.recordHistory(detached, null, Status.RECEIVED));
	}

	@Test
	@Transactional
	@DisplayName("createRequest writes the arrival row (null -> RECEIVED) in the very same transaction as the request")
	void createRequest_persistsTheArrivalRow_inTheSameTransaction() {
		DocumentDto document = new DocumentDto(null, "invoice.pdf", "application/pdf", 2048L, "hash",
				Instant.parse("2024-01-01T00:00:00Z"));
		CreateRequestDto createRequestDto = new CreateRequestDto(1L, "INVOICE", 100L, List.of(document));

		RequestDto created = requestService.createRequest(createRequestDto);

		List<RequestStatusHistory> history = requestStatusHistoryRepository
				.findByRequest_IdOrderByChangedAtAscIdAsc(created.id());
		assertEquals(1, history.size());
		assertNull(history.getFirst().getFromStatus());
		assertEquals(Status.RECEIVED, history.getFirst().getToStatus());
	}
}
