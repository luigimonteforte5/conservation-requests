package com.luigimonteforte.conservationrequests.service;

import com.luigimonteforte.conservationrequests.entity.Request;
import com.luigimonteforte.conservationrequests.entity.RequestStatusHistory;
import com.luigimonteforte.conservationrequests.entity.Status;
import com.luigimonteforte.conservationrequests.exception.ResourceNotFoundException;
import com.luigimonteforte.conservationrequests.mapper.RequestStatusHistoryMapper;
import com.luigimonteforte.conservationrequests.model.RequestStatusHistoryDto;
import com.luigimonteforte.conservationrequests.repository.RequestStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestStatusHistoryService {
	private final RequestStatusHistoryRepository requestStatusHistoryRepository;
	private final RequestStatusHistoryMapper requestStatusHistoryMapper;

	@Transactional(propagation = Propagation.MANDATORY)
	public void recordHistory(Request request, Status oldStatus, Status newStatus) {
		requestStatusHistoryRepository
				.save(RequestStatusHistory
						.builder()
						.fromStatus(oldStatus)
						.toStatus(newStatus)
						.request(request)
						.build());
	}

	@Transactional(readOnly = true)
	public List<RequestStatusHistoryDto> findByRequest(Long requestId){
		log.debug("Fetching status history for request {}", requestId);
		List<RequestStatusHistory> found = requestStatusHistoryRepository.findByRequest_IdOrderByChangedAtAscIdAsc(requestId);
		// Every request is born with an initial RECEIVED row, so an empty history can only mean the request
		// itself does not exist -- report it the same way the rest of the API reports a missing request.
		if (found.isEmpty()) {
			throw new ResourceNotFoundException("No Resource found with id %d".formatted(requestId));
		}
		return found.stream().map(requestStatusHistoryMapper::toDto).toList();
	}
}
