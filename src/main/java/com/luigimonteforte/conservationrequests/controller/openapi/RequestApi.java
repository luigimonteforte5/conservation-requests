package com.luigimonteforte.conservationrequests.controller.openapi;

import com.luigimonteforte.conservationrequests.entity.Status;
import com.luigimonteforte.conservationrequests.model.CreateRequestDto;
import com.luigimonteforte.conservationrequests.model.RequestDto;
import com.luigimonteforte.conservationrequests.model.RequestStatusHistoryDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * The published contract of /api/v1/requests: what each endpoint promises,
 * which status codes it can answer with, and what the payloads look like. It
 * carries the OpenAPI annotations so that
 * {@link com.luigimonteforte.conservationrequests.controller.RequestController}
 * is left with the logic alone — springdoc reads the annotations from the
 * implemented interface.
 */
@Tag(name = "Conservation requests", description = "Register conservation requests, follow them through their lifecycle, and look them up.")
public interface RequestApi {

	@Operation(summary = "Create a conservation request", description = "Registers a new request with its documents. The pair (producerId, externalId) must be unique.")
	@ApiResponse(responseCode = "201", description = "Request created; the Location header points at it", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RequestDto.class), examples = @ExampleObject(value = ApiExamples.RECEIVED)))
	@ApiResponse(responseCode = "400", description = "A field is missing or invalid; the body lists them under 'errors'", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class), examples = @ExampleObject(value = ApiExamples.VALIDATION_ERROR)))
	@ApiResponse(responseCode = "409", description = "A request with the same producerId and externalId already exists", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class), examples = @ExampleObject(value = ApiExamples.DUPLICATE_ERROR)))
	@UnauthorizedResponse
	ResponseEntity<RequestDto> createRequest(CreateRequestDto createRequestDto);

	@Operation(summary = "List requests", description = "Returns a page of requests, optionally filtered by producerId and status. "
			+ "Pagination metadata is returned under the 'page' object.")
	@ApiResponse(responseCode = "200", description = "A page of requests, possibly empty")
	@ApiResponse(responseCode = "400", description = "The status filter is not one of the known values", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class), examples = @ExampleObject(value = ApiExamples.BAD_FILTER_ERROR)))
	@UnauthorizedResponse
	ResponseEntity<PagedModel<RequestDto>> getRequests(
			@Schema(description = "Return only the requests of this producer", example = "7") Long producerId,
			@Schema(description = "Return only the requests currently in this status") Status status,
			@ParameterObject Pageable pageable);

	@Operation(summary = "Get a request by id", description = "Returns the request and the documents attached to it.")
	@ApiResponse(responseCode = "200", description = "The request", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RequestDto.class), examples = @ExampleObject(value = ApiExamples.RECEIVED)))
	@NotFoundResponse
	@UnauthorizedResponse
	ResponseEntity<RequestDto> getRequest(@Schema(example = "42") Long id);

	@Operation(summary = "Validate existing request", description = "Changes the status of an existing request from received to validated")
	@ApiResponse(responseCode = "200", description = "The request, now VALIDATED", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RequestDto.class), examples = @ExampleObject(value = ApiExamples.VALIDATED)))
	@NotFoundResponse
	@InvalidTransitionResponse
	@UnauthorizedResponse
	ResponseEntity<RequestDto> validateRequest(@Schema(example = "42") Long id);

	@Operation(summary = "Reject existing request", description = "Changes the status of an existing request to rejected")
	@ApiResponse(responseCode = "200", description = "The request, now REJECTED", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RequestDto.class), examples = @ExampleObject(value = ApiExamples.REJECTED)))
	@NotFoundResponse
	@InvalidTransitionResponse
	@UnauthorizedResponse
	ResponseEntity<RequestDto> rejectRequest(@Schema(example = "42") Long id);

	@Operation(summary = "Complete existing request", description = "Changes the status of a validated request to completed")
	@ApiResponse(responseCode = "200", description = "The request, now COMPLETED", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RequestDto.class), examples = @ExampleObject(value = ApiExamples.COMPLETED)))
	@NotFoundResponse
	@InvalidTransitionResponse
	@UnauthorizedResponse
	ResponseEntity<RequestDto> completeRequest(@Schema(example = "42") Long id);

	@Operation(summary = "Get a request's status history", description = "Returns the audit trail of a request's status changes, oldest first. "
			+ "The first entry records the request's arrival (from null to RECEIVED); every later entry records one transition.")
	@ApiResponse(responseCode = "200", description = "The status history, oldest first", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = RequestStatusHistoryDto.class))))
	@NotFoundResponse
	@UnauthorizedResponse
	ResponseEntity<List<RequestStatusHistoryDto>> getRequestStatusHistory(@Schema(example = "42") Long id);
}