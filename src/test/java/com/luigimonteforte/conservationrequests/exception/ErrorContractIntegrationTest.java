package com.luigimonteforte.conservationrequests.exception;

import com.jayway.jsonpath.JsonPath;
import com.luigimonteforte.conservationrequests.security.service.JwtService;
import com.luigimonteforte.conservationrequests.service.RequestService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Runs against a real servlet container on purpose. MockMvc does not perform the container's dispatch to
 * /error, which is where every unhandled failure is shaped into a response — so this whole family of bugs
 * is invisible to the MockMvc slices.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("Error contract")
class ErrorContractIntegrationTest {

    private static final String INTERNAL_MESSAGE = "column PASSWORD_HASH of table USERS does not exist";

    @LocalServerPort
    private int port;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private RequestService requestService;

    private final RestClient client = RestClient.create();

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private String bearer() {
        return "Bearer " + jwtService.generateToken("sa");
    }

    private static void assertIsProblemJson(ResponseEntity<String> response) {
        assertTrue(response.getHeaders().getContentType().isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON),
                () -> "expected application/problem+json but was " + response.getHeaders().getContentType());
    }

    @Test
    @DisplayName("answers an invalid body with 400, not with the 401 the error dispatch would otherwise produce")
    void invalidBody_answersWith400ProblemDetail_forAnAuthenticatedCaller() {
        String invalidBody = "{\"producerId\":null,\"documentType\":\"\",\"externalId\":null,\"documents\":[]}";

        ResponseEntity<String> response = client.post()
                .uri(url("/api/v1/requests"))
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .body(invalidBody)
                .retrieve()
                .onStatus(status -> true, (request, res) -> {
                })
                .toEntity(String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertIsProblemJson(response);
        assertEquals("/api/v1/requests", JsonPath.read(response.getBody(), "$.instance"),
                "instance must name the endpoint the caller asked for: /error would mean the error dispatch "
                        + "was re-authenticated and the real status was lost");

        Map<String, List<String>> errors = JsonPath.read(response.getBody(), "$.errors");
        assertEquals(Set.of("producerId", "documentType", "externalId", "documents"), errors.keySet(),
                "every violated field must be reported to the caller");
    }

    @Test
    @DisplayName("answers an unexpected failure with 500 and keeps the internal message out of the body")
    void unexpectedFailure_answersWith500ProblemDetail_withoutLeakingTheInternalMessage() {
        when(requestService.findById(any())).thenThrow(new IllegalStateException(INTERNAL_MESSAGE));

        ResponseEntity<String> response = client.get()
                .uri(url("/api/v1/requests/1"))
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .retrieve()
                .onStatus(status -> true, (request, res) -> {
                })
                .toEntity(String.class);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertIsProblemJson(response);
        assertFalse(response.getBody().contains(INTERNAL_MESSAGE),
                () -> "the body must not carry the internal failure message, but was: " + response.getBody());
    }

    @Test
    @DisplayName("keeps /error itself protected: only the container's internal ERROR dispatch is permitted")
    void errorPath_staysProtected_whenAskedForDirectly() {
        ResponseEntity<String> response = client.get()
                .uri(url("/error"))
                .retrieve()
                .onStatus(status -> true, (request, res) -> {
                })
                .toEntity(String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
}
