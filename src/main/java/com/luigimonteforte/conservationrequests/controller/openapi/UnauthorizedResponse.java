package com.luigimonteforte.conservationrequests.controller.openapi;

import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The 401 answered whenever the bearer token is missing, malformed or expired.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ApiResponse(responseCode = "401", description = "The bearer token is missing, malformed or expired",
        headers = @Header(name = "WWW-Authenticate", description = "The scheme the caller is expected to use",
                schema = @Schema(type = "string", example = "Bearer")),
        content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class),
                examples = @ExampleObject(value = ApiExamples.UNAUTHORIZED_ERROR)))
@interface UnauthorizedResponse {
}
