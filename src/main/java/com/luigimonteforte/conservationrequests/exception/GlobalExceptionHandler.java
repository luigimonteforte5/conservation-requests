package com.luigimonteforte.conservationrequests.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(Exception.class)
	public ProblemDetail handleGenericException(Exception e) {
		log.error("Unexpected error in unhandled exception", e);
		return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error occurred");
	}

	@ExceptionHandler(DuplicateRequestException.class)
	public ProblemDetail handleDuplicateRequest(DuplicateRequestException ex) {
		log.warn("Duplicate request rejected: {}", ex.getMessage());
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ProblemDetail handleResourceNotFound(ResourceNotFoundException ex) {
		log.warn("Resource not found: {}", ex.getMessage());
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(InvalidStateTransitionException.class)
	public ProblemDetail handleInvalidStateTransition(InvalidStateTransitionException ex) {
		log.warn("Invalid state transition rejected: {}", ex.getMessage());
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
	}

	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<ProblemDetail> handleBadCredentials(BadCredentialsException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
		return ResponseEntity
				.status(HttpStatus.UNAUTHORIZED)
				.header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
				.body(problem);
	}

	@ExceptionHandler(AccessDeniedException.class)
	public void handleAccessDenied(AccessDeniedException ex) throws AccessDeniedException {
		throw ex;
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest webRequest) {
		Map<String, List<String>> errors = ex
				.getBindingResult()
				.getFieldErrors()
				.stream()
				.collect(Collectors
						.groupingBy(FieldError::getField,
								Collectors.mapping(FieldError::getDefaultMessage, Collectors.toList())));
		ProblemDetail body = ex.getBody();
		body.setProperty("errors", errors);
		return handleExceptionInternal(ex, body, headers, status, webRequest);
	}
}