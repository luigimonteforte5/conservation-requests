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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestService {
    private final RequestRepository requestRepository;
    private final RequestMapper requestMapper;

    public RequestDto createRequest(CreateRequestDto createRequestDto) {
        checkIfRequestExists(createRequestDto);
        Request newRequest = requestMapper.toEntity(createRequestDto);
        Instant actualTime = Instant.now();
        newRequest.setCreatedAt(actualTime);
        newRequest.setUpdatedAt(actualTime);
        Request saved = requestRepository.save(newRequest);
        log.info("Created request {} for producerId={}, externalId={}", saved.getId(), saved.getProducerId(), saved.getExternalId());
        return requestMapper.toDto(saved);
    }

    public Page<RequestDto> getRequests(Long producerId, Status status, Pageable pageable) {
        log.debug("Fetching requests with producerId={}, status={}, page={}", producerId, status, pageable);
        Specification<Request> spec = RequestSpecification.hasProducerId(producerId).and(RequestSpecification.hasStatus(status));
        Page<Request> requests = requestRepository.findAll(spec, pageable);
        return requests.map(requestMapper::toDto);
    }

    public RequestDto findById(Long id) {
        return requestMapper.toDto(requestRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(String.format("No Resource found with id %d", id))));
    }

    public RequestDto changeStatus(Long id, Status target) {
        Request found = requestRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(String.format("No Resource found with id %d", id)));
        Status current = found.getStatus();
        if (!current.canTransitionTo(target)) {
            throw new InvalidStateTransitionException(String.format("Cannot transition request %d from %s to %s", id, current, target));
        }
        found.setStatus(target);
        found.setUpdatedAt(Instant.now());
        Request saved = requestRepository.save(found);
        log.info("Request {} transitioned from {} to {}", id, current, target);
        return requestMapper.toDto(saved);
    }

    private void checkIfRequestExists(CreateRequestDto createRequestDto) {
        Long producerId = createRequestDto.producerId();
        Long externalId = createRequestDto.externalId();
        if (requestRepository.existsByExternalIdAndProducerId(externalId, producerId)) {
            log.error("Request already exists");
            throw new DuplicateRequestException(String.format("Request with externalId %d and producerId %d already exists", externalId, producerId));
        }
    }
}
