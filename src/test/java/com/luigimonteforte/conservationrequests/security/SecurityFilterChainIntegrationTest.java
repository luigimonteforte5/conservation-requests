package com.luigimonteforte.conservationrequests.security;

import com.jayway.jsonpath.JsonPath;
import com.luigimonteforte.conservationrequests.config.AppSecurityProperties;
import com.luigimonteforte.conservationrequests.security.service.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Security filter chain")
class SecurityFilterChainIntegrationTest {

	/**
	 * Plaintext of the bcrypt hash configured as admin-password in application-test.yaml: the hash cannot be
	 * reversed, so the pair must be kept in sync by hand.
	 */
	private static final String ADMIN_PASSWORD = "test";
	private static final String PROTECTED_ENDPOINT = "/api/v1/requests";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private AppSecurityProperties securityProperties;

	@Test
	@DisplayName("answers a protected endpoint with a 401 ProblemDetail when no token is presented")
	void protectedEndpoint_returns401ProblemDetail_whenTokenIsMissing() throws Exception {
		mockMvc
				.perform(get(PROTECTED_ENDPOINT))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.title").value("Unauthorized"))
				.andExpect(jsonPath("$.detail").value("Authentication required"))
				.andExpect(jsonPath("$.instance").value(PROTECTED_ENDPOINT));
	}

	@Test
	@DisplayName("serves a protected endpoint when the token is valid")
	void protectedEndpoint_returns200_whenTokenIsValid() throws Exception {
		mockMvc
				.perform(get(PROTECTED_ENDPOINT)
						.header(HttpHeaders.AUTHORIZATION, bearer(jwtService.generateToken(securityProperties.adminUsername()))))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("rejects a protected endpoint when the token is not a JWT at all")
	void protectedEndpoint_returns401_whenTokenIsMalformed() throws Exception {
		mockMvc
				.perform(get(PROTECTED_ENDPOINT).header(HttpHeaders.AUTHORIZATION, bearer("not-a-jwt")))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("rejects a token we signed ourselves when it names a user that does not exist")
	void protectedEndpoint_returns401_whenTokenNamesUnknownUser() throws Exception {
		mockMvc
				.perform(get(PROTECTED_ENDPOINT)
						.header(HttpHeaders.AUTHORIZATION, bearer(jwtService.generateToken("ghost"))))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("rejects a token that carries a valid signature but no subject")
	void protectedEndpoint_returns401_whenTokenHasNoSubject() throws Exception {
		SecretKey key = Keys.hmacShaKeyFor(securityProperties.jwtSecret().getBytes(StandardCharsets.UTF_8));
		String subjectLessToken = Jwts.builder().claim("foo", "bar").signWith(key).compact();

		mockMvc
				.perform(get(PROTECTED_ENDPOINT).header(HttpHeaders.AUTHORIZATION, bearer(subjectLessToken)))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("does not disclose why a token was rejected")
	void protectedEndpoint_doesNotDiscloseRejectionReason_whenTokenIsRejected() throws Exception {
		mockMvc
				.perform(get(PROTECTED_ENDPOINT).header(HttpHeaders.AUTHORIZATION, bearer("not-a-jwt")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.detail").value("Authentication required"));
	}

	@Test
	@DisplayName("leaves the login endpoint public, and the token it issues opens a protected endpoint")
	void loginEndpoint_isPublic_andIssuesAUsableToken() throws Exception {
		MvcResult login = mockMvc
				.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(securityProperties.adminUsername(), ADMIN_PASSWORD)))
				.andExpect(status().isOk())
				.andReturn();

		String token = JsonPath.read(login.getResponse().getContentAsString(), "$.token");

		mockMvc
				.perform(get(PROTECTED_ENDPOINT).header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("leaves the OpenAPI description public")
	void apiDocs_isReachable_withoutAToken() throws Exception {
		mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
	}

	@Test
	@DisplayName("creates no session, as a stateless chain requires")
	void authenticatedRequest_createsNoSession() throws Exception {
		MvcResult result = mockMvc
				.perform(get(PROTECTED_ENDPOINT)
						.header(HttpHeaders.AUTHORIZATION, bearer(jwtService.generateToken(securityProperties.adminUsername()))))
				.andExpect(status().isOk())
				.andReturn();

		assertNull(result.getRequest().getSession(false));
	}

	private String bearer(String token) {
		return "Bearer " + token;
	}
}
