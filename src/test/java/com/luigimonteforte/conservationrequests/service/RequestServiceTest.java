package com.luigimonteforte.conservationrequests.service;

import com.luigimonteforte.conservationrequests.entity.Request;
import com.luigimonteforte.conservationrequests.entity.Status;
import com.luigimonteforte.conservationrequests.exception.DuplicateRequestException;
import com.luigimonteforte.conservationrequests.exception.InvalidStateTransitionException;
import com.luigimonteforte.conservationrequests.exception.ResourceNotFoundException;
import com.luigimonteforte.conservationrequests.mapper.RequestMapper;
import com.luigimonteforte.conservationrequests.model.CreateRequestDto;
import com.luigimonteforte.conservationrequests.model.DocumentDto;
import com.luigimonteforte.conservationrequests.model.RequestDto;
import com.luigimonteforte.conservationrequests.repository.RequestRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RequestService")
class RequestServiceTest {

	@Mock
	private RequestRepository requestRepository;

	@Mock
	private RequestMapper requestMapper;

	@InjectMocks
	private RequestService requestService;

	private static DocumentDto sampleDocumentDto() {
		return new DocumentDto(null, "file.pdf", "application/pdf", 1024L, "hash", Instant.now());
	}

	private static void assertIsBetween(Instant actual, Instant start, Instant end) {
		assertNotNull(actual);
		assertTrue(!actual.isBefore(start) && !actual.isAfter(end),
				() -> "expected %s to be between %s and %s".formatted(actual, start, end));
	}

	@Test
	@DisplayName("createRequest saves and returns the mapped DTO when no duplicate exists")
	void createRequest_savesAndReturnsDto_whenNoDuplicateExists() {
		CreateRequestDto createRequestDto = new CreateRequestDto(1L, "INVOICE", 100L, List.of(sampleDocumentDto()));
		Request mappedEntity = Request.builder().externalId(100L).producerId(1L).documentType("INVOICE").build();
		Request savedEntity = Request
				.builder()
				.id(10L)
				.externalId(100L)
				.producerId(1L)
				.documentType("INVOICE")
				.status(Status.RECEIVED)
				.build();
		RequestDto expectedDto = new RequestDto(10L, 100L, 1L, "INVOICE", Status.RECEIVED, null, null, List.of());

		when(requestRepository.existsByExternalIdAndProducerId(100L, 1L)).thenReturn(false);
		when(requestMapper.toEntity(createRequestDto)).thenReturn(mappedEntity);
		when(requestRepository.save(mappedEntity)).thenReturn(savedEntity);
		when(requestMapper.toDto(savedEntity)).thenReturn(expectedDto);

		RequestDto result = requestService.createRequest(createRequestDto);

		assertEquals(expectedDto, result);
		verify(requestRepository).save(mappedEntity);
	}

	@Test
	@DisplayName("createRequest sets createdAt and updatedAt to the current time before saving")
	void createRequest_setsCreatedAtAndUpdatedAt_toCurrentTime() {
		CreateRequestDto createRequestDto = new CreateRequestDto(1L, "INVOICE", 100L, List.of(sampleDocumentDto()));
		Request mappedEntity = Request.builder().externalId(100L).producerId(1L).documentType("INVOICE").build();
		Request savedEntity = Request.builder().id(10L).build();
		RequestDto expectedDto = new RequestDto(10L, 100L, 1L, "INVOICE", Status.RECEIVED, null, null, List.of());

		when(requestRepository.existsByExternalIdAndProducerId(100L, 1L)).thenReturn(false);
		when(requestMapper.toEntity(createRequestDto)).thenReturn(mappedEntity);
		when(requestRepository.save(mappedEntity)).thenReturn(savedEntity);
		when(requestMapper.toDto(savedEntity)).thenReturn(expectedDto);

		Instant before = Instant.now();
		requestService.createRequest(createRequestDto);
		Instant after = Instant.now();

		assertIsBetween(mappedEntity.getCreatedAt(), before, after);
		assertIsBetween(mappedEntity.getUpdatedAt(), before, after);
	}

	@Test
	@DisplayName("createRequest throws DuplicateRequestException when producerId + externalId already exist")
	void createRequest_throwsDuplicateRequestException_whenAlreadyExists() {
		CreateRequestDto createRequestDto = new CreateRequestDto(1L, "INVOICE", 100L, List.of(sampleDocumentDto()));
		when(requestRepository.existsByExternalIdAndProducerId(100L, 1L)).thenReturn(true);

		assertThrows(DuplicateRequestException.class, () -> requestService.createRequest(createRequestDto));

		verify(requestRepository, never()).save(any());
	}

	@Test
	@DisplayName("createRequest still reports a duplicate when the pre-check sees none but the database rejects the insert")
	void createRequest_throwsDuplicateRequestException_whenSaveViolatesTheUniqueConstraint() {
		CreateRequestDto createRequestDto = new CreateRequestDto(1L, "INVOICE", 100L, List.of(sampleDocumentDto()));
		Request mappedEntity = Request.builder().externalId(100L).producerId(1L).documentType("INVOICE").build();
		DataIntegrityViolationException constraintViolation = new DataIntegrityViolationException(
				"uk_request_producer_external");

		when(requestRepository.existsByExternalIdAndProducerId(100L, 1L)).thenReturn(false);
		when(requestMapper.toEntity(createRequestDto)).thenReturn(mappedEntity);
		when(requestRepository.save(mappedEntity)).thenThrow(constraintViolation);

		assertThrows(DuplicateRequestException.class, () -> requestService.createRequest(createRequestDto));
	}

	@Test
	@DisplayName("createRequest keeps the constraint violation as the cause for diagnostics")
	void createRequest_keepsConstraintViolationAsCause_whenSaveIsRejected() {
		CreateRequestDto createRequestDto = new CreateRequestDto(1L, "INVOICE", 100L, List.of(sampleDocumentDto()));
		Request mappedEntity = Request.builder().externalId(100L).producerId(1L).documentType("INVOICE").build();
		DataIntegrityViolationException constraintViolation = new DataIntegrityViolationException(
				"uk_request_producer_external");

		when(requestRepository.existsByExternalIdAndProducerId(100L, 1L)).thenReturn(false);
		when(requestMapper.toEntity(createRequestDto)).thenReturn(mappedEntity);
		when(requestRepository.save(mappedEntity)).thenThrow(constraintViolation);

		DuplicateRequestException thrown = assertThrows(DuplicateRequestException.class,
				() -> requestService.createRequest(createRequestDto));

		assertSame(constraintViolation, thrown.getCause());
	}

	@Test
	@DisplayName("createRequest reports a duplicate identically whether the pre-check or the database catches it")
	void createRequest_reportsTheSameMessage_whicheverDuplicatePathFires() {
		CreateRequestDto createRequestDto = new CreateRequestDto(1L, "INVOICE", 100L, List.of(sampleDocumentDto()));
		Request mappedEntity = Request.builder().externalId(100L).producerId(1L).documentType("INVOICE").build();

		when(requestRepository.existsByExternalIdAndProducerId(100L, 1L)).thenReturn(true);
		DuplicateRequestException caughtByPreCheck = assertThrows(DuplicateRequestException.class,
				() -> requestService.createRequest(createRequestDto));

		when(requestRepository.existsByExternalIdAndProducerId(100L, 1L)).thenReturn(false);
		when(requestMapper.toEntity(createRequestDto)).thenReturn(mappedEntity);
		when(requestRepository.save(mappedEntity))
				.thenThrow(new DataIntegrityViolationException("uk_request_producer_external"));
		DuplicateRequestException caughtByDatabase = assertThrows(DuplicateRequestException.class,
				() -> requestService.createRequest(createRequestDto));

		assertEquals(caughtByPreCheck.getMessage(), caughtByDatabase.getMessage());
	}

	@Test
	@DisplayName("findById returns the mapped DTO when the request exists")
	void findById_returnsMappedDto_whenFound() {
		Request entity = Request.builder().id(5L).status(Status.RECEIVED).build();
		RequestDto dto = new RequestDto(5L, null, null, null, Status.RECEIVED, null, null, null);
		when(requestRepository.findById(5L)).thenReturn(Optional.of(entity));
		when(requestMapper.toDto(entity)).thenReturn(dto);

		assertEquals(dto, requestService.findById(5L));
	}

	@Test
	@DisplayName("findById throws ResourceNotFoundException when the id does not exist")
	void findById_throwsResourceNotFoundException_whenMissing() {
		when(requestRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> requestService.findById(99L));
	}

	@Test
	@DisplayName("getRequests maps the repository page into a page of DTOs")
	void getRequests_mapsRepositoryPageToDtoPage() {
		Pageable pageable = PageRequest.of(0, 10);
		Request entity = Request.builder().id(1L).producerId(1L).status(Status.RECEIVED).build();
		RequestDto dto = new RequestDto(1L, null, 1L, null, Status.RECEIVED, null, null, null);
		Page<Request> repoPage = new PageImpl<>(List.of(entity), pageable, 1);

		when(requestRepository.findAll(ArgumentMatchers.<Specification<Request>> any(), eq(pageable)))
				.thenReturn(repoPage);
		when(requestMapper.toDto(entity)).thenReturn(dto);

		Page<RequestDto> result = requestService.getRequests(1L, Status.RECEIVED, pageable);

		assertEquals(List.of(dto), result.getContent());
	}

	@ParameterizedTest(name = "{0} -> {1}")
	@MethodSource("legalTransitions")
	@DisplayName("changeStatus persists the new status for every legal transition")
	void changeStatus_updatesAndSaves_whenTransitionIsLegal(Status from, Status to) {
		Request entity = Request.builder().id(5L).status(from).build();
		Request saved = Request.builder().id(5L).status(to).build();
		RequestDto dto = new RequestDto(5L, null, null, null, to, null, null, null);

		when(requestRepository.findWithLockById(5L)).thenReturn(Optional.of(entity));
		when(requestRepository.save(entity)).thenReturn(saved);
		when(requestMapper.toDto(saved)).thenReturn(dto);

		RequestDto result = requestService.changeStatus(5L, to);

		assertEquals(dto, result);
		assertEquals(to, entity.getStatus());
	}

	@Test
	@DisplayName("changeStatus updates updatedAt to the current time while leaving createdAt untouched")
	void changeStatus_updatesUpdatedAt_whileKeepingCreatedAt() {
		Instant originalCreatedAt = Instant.parse("2020-01-01T00:00:00Z");
		Request entity = Request.builder().id(5L).status(Status.RECEIVED).createdAt(originalCreatedAt).build();
		Request saved = Request.builder().id(5L).status(Status.VALIDATED).build();
		RequestDto dto = new RequestDto(5L, null, null, null, Status.VALIDATED, null, null, null);

		when(requestRepository.findWithLockById(5L)).thenReturn(Optional.of(entity));
		when(requestRepository.save(entity)).thenReturn(saved);
		when(requestMapper.toDto(saved)).thenReturn(dto);

		Instant before = Instant.now();
		requestService.changeStatus(5L, Status.VALIDATED);
		Instant after = Instant.now();

		assertEquals(originalCreatedAt, entity.getCreatedAt());
		assertIsBetween(entity.getUpdatedAt(), before, after);
	}

	@Test
	@DisplayName("changeStatus reads the request through the locking query, never through a plain read")
	void changeStatus_readsThroughTheLockingQuery_neverThroughAPlainRead() {
		Request entity = Request.builder().id(5L).status(Status.RECEIVED).build();
		Request saved = Request.builder().id(5L).status(Status.VALIDATED).build();
		RequestDto dto = new RequestDto(5L, null, null, null, Status.VALIDATED, null, null, null);

		when(requestRepository.findWithLockById(5L)).thenReturn(Optional.of(entity));
		when(requestRepository.save(entity)).thenReturn(saved);
		when(requestMapper.toDto(saved)).thenReturn(dto);

		requestService.changeStatus(5L, Status.VALIDATED);

		verify(requestRepository).findWithLockById(5L);
		verify(requestRepository, never()).findById(any());
	}

	static Stream<Arguments> legalTransitions() {
		return Stream
				.of(Arguments.of(Status.RECEIVED, Status.VALIDATED), Arguments.of(Status.RECEIVED, Status.REJECTED),
						Arguments.of(Status.VALIDATED, Status.COMPLETED));
	}

	@Test
	@DisplayName("changeStatus throws ResourceNotFoundException when the id does not exist")
	void changeStatus_throwsResourceNotFoundException_whenMissing() {
		when(requestRepository.findWithLockById(5L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> requestService.changeStatus(5L, Status.VALIDATED));
	}

	@ParameterizedTest(name = "{0} -> {1}")
	@MethodSource("illegalTransitions")
	@DisplayName("changeStatus throws InvalidStateTransitionException for every illegal transition and does not save")
	void changeStatus_throwsInvalidStateTransitionException_forIllegalTransitions(Status from, Status to) {
		Request entity = Request.builder().id(5L).status(from).build();
		when(requestRepository.findWithLockById(5L)).thenReturn(Optional.of(entity));

		assertThrows(InvalidStateTransitionException.class, () -> requestService.changeStatus(5L, to));

		verify(requestRepository, never()).save(any());
	}

	static Stream<Arguments> illegalTransitions() {
		return Stream
				.of(Arguments.of(Status.RECEIVED, Status.RECEIVED), Arguments.of(Status.RECEIVED, Status.COMPLETED),
						Arguments.of(Status.VALIDATED, Status.RECEIVED),
						Arguments.of(Status.VALIDATED, Status.REJECTED),
						Arguments.of(Status.REJECTED, Status.VALIDATED),
						Arguments.of(Status.REJECTED, Status.COMPLETED),
						Arguments.of(Status.COMPLETED, Status.VALIDATED),
						Arguments.of(Status.COMPLETED, Status.REJECTED));
	}
}
