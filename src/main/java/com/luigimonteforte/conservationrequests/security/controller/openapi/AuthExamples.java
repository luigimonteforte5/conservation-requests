package com.luigimonteforte.conservationrequests.security.controller.openapi;

/**
 * Payload examples for the login endpoint, kept out of {@link AuthApi} so that they do not become part of its public
 * surface: fields declared on an interface are implicitly public. They mirror what {@code GlobalExceptionHandler}
 * and {@code AuthService} actually return.
 */
final class AuthExamples {

    private AuthExamples() {
    }

    static final String TOKEN = """
            {
              "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJvcGVyYXRvciIsImlhdCI6MTc1MzAwMDAwMCwiZXhwIjoxNzUzMDAzNjAwfQ.Qm9ndXNTaWduYXR1cmVGb3JEb2N1bWVudGF0aW9u"
            }""";

    static final String MISSING_FIELD_ERROR = """
            {
              "title": "Bad Request",
              "status": 400,
              "detail": "Invalid request content.",
              "instance": "/api/v1/auth/login",
              "errors": {
                "password": ["must not be blank"]
              }
            }""";

    static final String BAD_CREDENTIALS_ERROR = """
            {
              "title": "Unauthorized",
              "status": 401,
              "detail": "Invalid username or password",
              "instance": "/api/v1/auth/login"
            }""";
}
