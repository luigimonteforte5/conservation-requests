package com.luigimonteforte.conservationrequests.security.controller.openapi;

import com.luigimonteforte.conservationrequests.security.model.LoginRequest;
import com.luigimonteforte.conservationrequests.security.model.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

/**
 * The published contract of /api/v1/auth: the one endpoint that hands out the bearer token every other endpoint
 * asks for. It carries the OpenAPI annotations so that
 * {@link com.luigimonteforte.conservationrequests.security.controller.AuthController} is left with the logic
 * alone — springdoc reads the annotations from the implemented interface.
 */
@Tag(name = "Authentication", description = "Exchange credentials for the bearer token the other endpoints require.")
public interface AuthApi {

	@Operation(summary = "Log in", description = "Verifies the credentials and returns a signed JWT to send as "
			+ "'Authorization: Bearer <token>' on every other endpoint.")
	@ApiResponse(responseCode = "200", description = "The credentials were accepted", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = LoginResponse.class), examples = @ExampleObject(value = AuthExamples.TOKEN)))
	@ApiResponse(responseCode = "400", description = "Username or password missing from the body", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class), examples = @ExampleObject(value = AuthExamples.MISSING_FIELD_ERROR)))
	@ApiResponse(responseCode = "401", description = "The credentials were rejected. The body says no more than "
			+ "that, on purpose: telling the caller which half was wrong would confirm that an account exists.",
			headers = @Header(name = "WWW-Authenticate", description = "The scheme the caller is expected to use", schema = @Schema(type = "string", example = "Bearer")),
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class), examples = @ExampleObject(value = AuthExamples.BAD_CREDENTIALS_ERROR)))
	@SecurityRequirements
	ResponseEntity<LoginResponse> login(LoginRequest loginRequest);
}
