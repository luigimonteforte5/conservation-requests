package com.luigimonteforte.conservationrequests.repository;

import com.luigimonteforte.conservationrequests.entity.Request;
import com.luigimonteforte.conservationrequests.entity.RequestStatusHistory;
import com.luigimonteforte.conservationrequests.entity.Status;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("RequestStatusHistoryRepository")
class RequestStatusHistoryRepositoryTest {

	@Autowired
	private RequestStatusHistoryRepository requestStatusHistoryRepository;

	@Autowired
	private RequestRepository requestRepository;

	@Autowired
	private EntityManager entityManager;

	private Request persistedRequest(Long producerId, Long externalId) {
		Request request = Request.builder()
				.externalId(externalId)
				.producerId(producerId)
				.documentType("INVOICE")
				.status(Status.RECEIVED)
				.build();
		return requestRepository.save(request);
	}

	private void record(Request request, Status from, Status to) {
		requestStatusHistoryRepository.save(RequestStatusHistory.builder()
				.request(request)
				.fromStatus(from)
				.toStatus(to)
				.build());
	}

	@Test
	@DisplayName("returns a request's history oldest-first and leaves out other requests' rows")
	void findByRequest_returnsOwnHistoryInOrder() {
		Request request = persistedRequest(1L, 100L);
		Request other = persistedRequest(2L, 200L);
		record(request, null, Status.RECEIVED);
		record(request, Status.RECEIVED, Status.VALIDATED);
		record(other, null, Status.RECEIVED);
		entityManager.flush();
		entityManager.clear();

		List<RequestStatusHistory> history = requestStatusHistoryRepository
				.findByRequest_IdOrderByChangedAtAscIdAsc(request.getId());

		assertEquals(2, history.size());
		// insertion order is preserved: the id tie-breaker settles rows that share a changedAt instant
		assertNull(history.getFirst().getFromStatus());
		assertEquals(Status.RECEIVED, history.getFirst().getToStatus());
		assertEquals(Status.RECEIVED, history.get(1).getFromStatus());
		assertEquals(Status.VALIDATED, history.get(1).getToStatus());
	}

	@Test
	@DisplayName("returns an empty list for a request that has no history rows")
	void findByRequest_returnsEmpty_whenNoRows() {
		Request request = persistedRequest(1L, 100L);
		entityManager.flush();
		entityManager.clear();

		assertTrue(requestStatusHistoryRepository
				.findByRequest_IdOrderByChangedAtAscIdAsc(request.getId())
				.isEmpty());
	}

	@Test
	@DisplayName("Hibernate stamps changedAt when a history row is first saved")
	void save_stampsChangedAt() {
		Request request = persistedRequest(1L, 100L);

		RequestStatusHistory saved = requestStatusHistoryRepository.save(RequestStatusHistory.builder()
				.request(request)
				.toStatus(Status.RECEIVED)
				.build());
		entityManager.flush();

		assertEquals(request.getId(), saved.getRequest().getId());
		assertNotNull(saved.getChangedAt());
	}
}
