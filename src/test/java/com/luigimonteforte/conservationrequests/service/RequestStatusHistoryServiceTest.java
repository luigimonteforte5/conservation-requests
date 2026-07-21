package com.luigimonteforte.conservationrequests.service;

import com.luigimonteforte.conservationrequests.entity.Request;
import com.luigimonteforte.conservationrequests.entity.RequestStatusHistory;
import com.luigimonteforte.conservationrequests.entity.Status;
import com.luigimonteforte.conservationrequests.exception.ResourceNotFoundException;
import com.luigimonteforte.conservationrequests.mapper.RequestStatusHistoryMapper;
import com.luigimonteforte.conservationrequests.model.RequestStatusHistoryDto;
import com.luigimonteforte.conservationrequests.repository.RequestStatusHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RequestStatusHistoryService")
class RequestStatusHistoryServiceTest {

	@Mock
	private RequestStatusHistoryRepository requestStatusHistoryRepository;

	@Mock
	private RequestStatusHistoryMapper requestStatusHistoryMapper;

	@InjectMocks
	private RequestStatusHistoryService requestStatusHistoryService;

	@Test
	@DisplayName("recordHistory saves a row carrying the request and the from/to statuses it is given")
	void recordHistory_savesRowWithRequestAndBothStatuses() {
		Request request = Request.builder().id(5L).status(Status.VALIDATED).build();

		requestStatusHistoryService.recordHistory(request, Status.RECEIVED, Status.VALIDATED);

		ArgumentCaptor<RequestStatusHistory> captor = ArgumentCaptor.forClass(RequestStatusHistory.class);
		verify(requestStatusHistoryRepository).save(captor.capture());
		RequestStatusHistory saved = captor.getValue();
		assertEquals(request, saved.getRequest());
		assertEquals(Status.RECEIVED, saved.getFromStatus());
		assertEquals(Status.VALIDATED, saved.getToStatus());
	}

	@Test
	@DisplayName("recordHistory keeps a null fromStatus, so the very first row records an arrival, not a transition")
	void recordHistory_keepsNullFromStatus_forTheInitialRow() {
		Request request = Request.builder().id(5L).status(Status.RECEIVED).build();

		requestStatusHistoryService.recordHistory(request, null, Status.RECEIVED);

		ArgumentCaptor<RequestStatusHistory> captor = ArgumentCaptor.forClass(RequestStatusHistory.class);
		verify(requestStatusHistoryRepository).save(captor.capture());
		assertNull(captor.getValue().getFromStatus());
		assertEquals(Status.RECEIVED, captor.getValue().getToStatus());
	}

	@Test
	@DisplayName("findByRequest returns the mapped history when the request has rows")
	void findByRequest_returnsMappedHistory_whenRowsExist() {
		RequestStatusHistory first = RequestStatusHistory.builder().toStatus(Status.RECEIVED).build();
		RequestStatusHistory second = RequestStatusHistory.builder().fromStatus(Status.RECEIVED).toStatus(Status.VALIDATED).build();
		RequestStatusHistoryDto firstDto = new RequestStatusHistoryDto(null, Status.RECEIVED, Instant.now());
		RequestStatusHistoryDto secondDto = new RequestStatusHistoryDto(Status.RECEIVED, Status.VALIDATED, Instant.now());

		when(requestStatusHistoryRepository.findByRequest_IdOrderByChangedAtAscIdAsc(5L))
				.thenReturn(List.of(first, second));
		when(requestStatusHistoryMapper.toDto(first)).thenReturn(firstDto);
		when(requestStatusHistoryMapper.toDto(second)).thenReturn(secondDto);

		List<RequestStatusHistoryDto> result = requestStatusHistoryService.findByRequest(5L);

		assertEquals(List.of(firstDto, secondDto), result);
	}

	@Test
	@DisplayName("findByRequest throws ResourceNotFoundException when there is no history, because a real request always has at least its arrival row")
	void findByRequest_throwsResourceNotFound_whenEmpty() {
		when(requestStatusHistoryRepository.findByRequest_IdOrderByChangedAtAscIdAsc(99L)).thenReturn(List.of());

		assertThrows(ResourceNotFoundException.class, () -> requestStatusHistoryService.findByRequest(99L));
	}
}
