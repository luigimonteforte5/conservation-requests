package com.luigimonteforte.conservationrequests.controller;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.luigimonteforte.conservationrequests.model.DocumentDto;
import com.luigimonteforte.conservationrequests.model.RequestDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The OpenAPI annotations live on {@link com.luigimonteforte.conservationrequests.controller.openapi.RequestApi},
 * not on the controller that implements it. Nothing in the
 * compiler enforces that springdoc actually reads them from there: were it to stop, the application would still
 * build and every other test would stay green while the published documentation quietly emptied out.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("OpenAPI documentation")
class OpenApiDocumentationTest {

    @LocalServerPort
    private int port;

    private final RestClient client = RestClient.create();

    private String apiDocs;

    @BeforeEach
    void fetchApiDocs() {
        apiDocs = client.get()
                .uri("http://localhost:" + port + "/v3/api-docs")
                .retrieve()
                .body(String.class);
    }

    @Test
    @DisplayName("carries the operation summaries declared on the interface")
    void documents_theOperationSummaries() {
        assertNotNull(apiDocs);
        assertEquals("Validate existing request",
                JsonPath.read(apiDocs, "$.paths['/api/v1/requests/{id}/validate'].patch.summary"));
        assertEquals("Create a conservation request",
                JsonPath.read(apiDocs, "$.paths['/api/v1/requests'].post.summary"));
    }

    @Test
    @DisplayName("documents the error codes each endpoint can answer with, not just the happy path")
    void documents_theErrorResponses() {
        assertNotNull(JsonPath.read(apiDocs, "$.paths['/api/v1/requests/{id}/validate'].patch.responses['404']"));
        assertNotNull(JsonPath.read(apiDocs, "$.paths['/api/v1/requests/{id}/validate'].patch.responses['409']"));
        assertNotNull(JsonPath.read(apiDocs, "$.paths['/api/v1/requests'].post.responses['409']"));
        assertThrows(PathNotFoundException.class,
                () -> JsonPath.read(apiDocs, "$.paths['/api/v1/requests/{id}'].get.responses['409']"),
                "a plain read cannot conflict: documenting a 409 there would describe an impossible answer");
    }

    @Test
    @DisplayName("shows each transition returning its own status, instead of the enum's first value everywhere")
    void documents_theResultingStatus_perTransition() {
        assertEquals("VALIDATED", documentedStatusAfter("validate"));
        assertEquals("REJECTED", documentedStatusAfter("reject"));
        assertEquals("COMPLETED", documentedStatusAfter("complete"));
    }

    @Test
    @DisplayName("spells the pagination out as page/size/sort instead of one opaque 'pageable' object")
    void documents_paginationAsSeparateOptionalParameters() {
        List<String> names = JsonPath.read(apiDocs, "$.paths['/api/v1/requests'].get.parameters[*].name");
        // compared as a set: the order springdoc emits them in is not part of the contract
        assertEquals(Set.of("producerId", "status", "page", "size", "sort"), Set.copyOf(names));

        List<Boolean> required = JsonPath.read(apiDocs, "$.paths['/api/v1/requests'].get.parameters[*].required");
        assertTrue(required.stream().noneMatch(Boolean::booleanValue),
                () -> "every filter and pagination parameter is optional, but got " + required);
    }

    @Test
    @DisplayName("illustrates the errors with what the handler really returns, not the empty ProblemDetail schema")
    void documents_errorsWithRealisticExamples() {
        Map<String, Object> notFound = JsonPath.read(apiDocs,
                "$.paths['/api/v1/requests/{id}'].get.responses['404'].content['application/problem+json'].example");
        assertEquals("Not Found", notFound.get("title"));
        assertEquals(404, notFound.get("status"));

        Map<String, Object> conflict = JsonPath.read(apiDocs,
                "$.paths['/api/v1/requests/{id}/validate'].patch.responses['409'].content['application/problem+json'].example");
        assertEquals("Conflict", conflict.get("title"));

        assertFalse(notFound.containsKey("type"),
                "the serialized ProblemDetail omits 'type' while it stays the default about:blank, so an example "
                        + "that shows it would describe a field the caller never receives");
    }

    @Test
    @DisplayName("keeps the hand-written examples in step with the DTOs they claim to illustrate")
    void examples_matchTheShapeOfTheDtos() {
        Map<String, Object> example = JsonPath.read(apiDocs,
                "$.paths['/api/v1/requests/{id}'].get.responses['200'].content['application/json'].example");
        assertEquals(recordComponentsOf(RequestDto.class), example.keySet(),
                "the example is written by hand and nothing but this test notices when a field is added or renamed");

        List<Map<String, Object>> documents = JsonPath.read(apiDocs,
                "$.paths['/api/v1/requests/{id}'].get.responses['200'].content['application/json'].example.documents");
        assertEquals(recordComponentsOf(DocumentDto.class), documents.getFirst().keySet());
    }

    private static Set<String> recordComponentsOf(Class<? extends Record> type) {
        return Arrays.stream(type.getRecordComponents()).map(RecordComponent::getName).collect(Collectors.toSet());
    }

    @Test
    @DisplayName("declares the bearer scheme and requires it everywhere except on login")
    void documents_theBearerRequirement_andExemptsLogin() {
        assertEquals("bearer", JsonPath.read(apiDocs, "$.components.securitySchemes.bearerAuth.scheme"));

        // A root-level requirement applies to every operation that does not override it, which is why the
        // protected endpoints carry no 'security' of their own.
        List<Map<String, Object>> rootRequirement = JsonPath.read(apiDocs, "$.security");
        assertEquals(1, rootRequirement.size());
        assertTrue(rootRequirement.getFirst().containsKey("bearerAuth"),
                () -> "the document must require bearerAuth by default, but was " + rootRequirement);

        List<?> loginRequirement = JsonPath.read(apiDocs, "$.paths['/api/v1/auth/login'].post.security");
        assertTrue(loginRequirement.isEmpty(),
                () -> "login is permitAll: an empty requirement is how OpenAPI spells 'no authentication here', "
                        + "but was " + loginRequirement);
    }

    private String documentedStatusAfter(String transition) {
        return JsonPath.read(apiDocs, "$.paths['/api/v1/requests/{id}/" + transition
                + "'].patch.responses['200'].content['application/json'].example.status");
    }
}
