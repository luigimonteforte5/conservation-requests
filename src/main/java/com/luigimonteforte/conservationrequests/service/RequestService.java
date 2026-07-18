package com.luigimonteforte.conservationrequests.service;

import com.luigimonteforte.conservationrequests.entity.Request;
import com.luigimonteforte.conservationrequests.entity.Status;
import com.luigimonteforte.conservationrequests.exception.DuplicateRequestException;
import com.luigimonteforte.conservationrequests.exception.InvalidStateTransitionException;
import com.luigimonteforte.conservationrequests.exception.ResourceNotFoundException;
import com.luigimonteforte.conservationrequests.mapper.RequestMapper;
import com.luigimonteforte.conservationrequests.model.CreateRequestDto;
import com.luigimonteforte.conservationrequests.model.RequestDto;
import com.luigimonteforte.conservationrequests.repository.RequestRepository;
import com.luigimonteforte.conservationrequests.utils.RequestSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestService {
	private final RequestRepository requestRepository;
	private final RequestMapper requestMapper;

	public RequestDto findById(Long id) {
		return requestMapper.toDto(getRequestOrThrow(id));
	}

	public RequestDto createRequest(CreateRequestDto createRequestDto) {
		checkIfRequestExists(createRequestDto);
		Request newRequest = requestMapper.toEntity(createRequestDto);
		try {
			Request saved = requestRepository.save(newRequest);
			log.info("Created request {} for producerId={}, externalId={}", saved.getId(), saved.getProducerId(),
							saved.getExternalId());
			return requestMapper.toDto(saved);
		} catch (DataIntegrityViolationException e) {
			throw new DuplicateRequestException(
					duplicateMessage(createRequestDto.producerId(), createRequestDto.externalId()), e);
		}
	}

	public Page<RequestDto> getRequests(Long producerId, Status status, Pageable pageable) {
		log.debug("Fetching requests with producerId={}, status={}, page={}", producerId, status, pageable);
		Specification<Request> spec = RequestSpecification
				.hasProducerId(producerId)
				.and(RequestSpecification.hasStatus(status));
		Page<Request> requests = requestRepository.findAll(spec, pageable);
		return requests.map(requestMapper::toDto);
	}

	@Transactional
	public RequestDto changeStatus(Long id, Status newStatus) {
		Request found = getRequestForUpdateOrThrow(id);
		Status current = found.getStatus();
		checkTransitionIsAllowed(id, current, newStatus);
		found.setStatus(newStatus);
		Request updated = requestRepository.save(found);
		log.info("Request with id {} has been updated from {} to {}", id, current, newStatus);
		return requestMapper.toDto(updated);
	}

	private void checkIfRequestExists(CreateRequestDto requestDto) {
		Long prodId = requestDto.producerId();
		Long extId = requestDto.externalId();
		if (requestRepository.existsByExternalIdAndProducerId(extId, prodId)) {
			throw new DuplicateRequestException(duplicateMessage(prodId, extId));
		}
	}

	private void checkTransitionIsAllowed(Long id, Status current, Status newStatus) {
		if (!current.canTransitionTo(newStatus)) {
			throw new InvalidStateTransitionException(
					String.format("Cannot transition request %d from %s to %s", id, current, newStatus));
		}
	}

	private Request getRequestForUpdateOrThrow(Long id) {
		return requestRepository
				.findWithLockById(id)
				.orElseThrow(() -> new ResourceNotFoundException(String.format("No Resource found with id %d", id)));
	}

	private Request getRequestOrThrow(Long id) {
		return requestRepository
				.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(String.format("No Resource found with id %d", id)));
	}

	private String duplicateMessage(Long producerId, Long externalId) {
		return String.format("Request with externalId %d and producerId %d already exists", externalId, producerId);
	}
}
