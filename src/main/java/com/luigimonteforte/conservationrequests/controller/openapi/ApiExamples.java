package com.luigimonteforte.conservationrequests.controller.openapi;

/**
 * Payload examples for the generated documentation, kept out of {@link RequestApi} so that they do not become part
 * of its public surface: fields declared on an interface are implicitly public.
 * <p>
 * Only the status differs between the success payloads, so the body is split around it — concatenating string
 * literals is still a compile-time constant, which is what an annotation argument has to be. The error examples
 * mirror what {@code GlobalExceptionHandler} actually returns; the bare ProblemDetail schema would only show
 * "string" and an empty property bag.
 */
final class ApiExamples {

    private ApiExamples() {
    }

    private static final String HEAD = """
            {
              "id": 42,
              "externalId": 10001,
              "producerId": 7,
              "documentType": "INVOICE",
              "status": \"""";

    private static final String TAIL = """
            ",
              "createdAt": "2026-07-19T09:15:00Z",
              "updatedAt": "2026-07-19T09:20:00Z",
              "documents": [
                {
                  "id": 128,
                  "fileName": "invoice-2026-0001.pdf",
                  "mimeType": "application/pdf",
                  "fileSize": 20480,
                  "hash": "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08",
                  "documentDate": "2026-07-18T00:00:00Z"
                }
              ]
            }""";

    static final String RECEIVED = HEAD + "RECEIVED" + TAIL;
    static final String VALIDATED = HEAD + "VALIDATED" + TAIL;
    static final String REJECTED = HEAD + "REJECTED" + TAIL;
    static final String COMPLETED = HEAD + "COMPLETED" + TAIL;

    static final String VALIDATION_ERROR = """
            {
              "title": "Bad Request",
              "status": 400,
              "detail": "Invalid request content.",
              "instance": "/api/v1/requests",
              "errors": {
                "documentType": ["must not be blank"],
                "documents[0].fileSize": ["must be greater than 0"]
              }
            }""";

    static final String DUPLICATE_ERROR = """
            {
              "title": "Conflict",
              "status": 409,
              "detail": "Request with externalId 10001 and producerId 7 already exists",
              "instance": "/api/v1/requests"
            }""";

    static final String TRANSITION_ERROR = """
            {
              "title": "Conflict",
              "status": 409,
              "detail": "Cannot transition request 42 from COMPLETED to VALIDATED",
              "instance": "/api/v1/requests/42/validate"
            }""";

    static final String NOT_FOUND_ERROR = """
            {
              "title": "Not Found",
              "status": 404,
              "detail": "No Resource found with id 99",
              "instance": "/api/v1/requests/99"
            }""";

    static final String UNAUTHORIZED_ERROR = """
            {
              "title": "Unauthorized",
              "status": 401,
              "detail": "Authentication required",
              "instance": "/api/v1/requests/42"
            }""";

    static final String BAD_FILTER_ERROR = """
            {
              "title": "Bad Request",
              "status": 400,
              "detail": "Failed to convert 'status' with value: 'NOT_A_STATUS'",
              "instance": "/api/v1/requests"
            }""";
}
