package com.luigimonteforte.conservationrequests.security.filter;

import com.luigimonteforte.conservationrequests.security.service.JwtService;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter")
class JwtAuthenticationFilterTest {

    private static final String TOKEN = "a.jwt.token";

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("continues the chain without authenticating when the Authorization header is missing")
    void doFilterInternal_continuesWithoutAuthenticating_whenHeaderIsMissing() throws Exception {
        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    @DisplayName("continues the chain without authenticating when the Authorization header is not a Bearer one")
    void doFilterInternal_continuesWithoutAuthenticating_whenHeaderIsNotBearer() throws Exception {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Basic YWRtaW46czNjcmV0");

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    @DisplayName("populates the security context with an authenticated token when the JWT is valid")
    void doFilterInternal_populatesSecurityContext_whenTokenIsValid() throws Exception {
        UserDetails userDetails = User.withUsername("admin").password("irrelevant").authorities("ROLE_ADMIN").build();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN);
        when(jwtService.extractUsername(TOKEN)).thenReturn("admin");
        when(userDetailsService.loadUserByUsername("admin")).thenReturn(userDetails);

        filter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.isAuthenticated());
        assertSame(userDetails, authentication.getPrincipal());
        assertNull(authentication.getCredentials());
        assertNotNull(authentication.getDetails());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("continues the chain unauthenticated when the JWT is rejected")
    void doFilterInternal_continuesUnauthenticated_whenTokenIsRejected() throws Exception {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN);
        when(jwtService.extractUsername(TOKEN)).thenThrow(new MalformedJwtException("malformed token"));

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(userDetailsService);
    }

    @Test
    @DisplayName("continues the chain unauthenticated when the Bearer header carries no token")
    void doFilterInternal_continuesUnauthenticated_whenBearerTokenIsBlank() throws Exception {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer ");

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    @DisplayName("continues the chain unauthenticated when the token names a user that no longer exists")
    void doFilterInternal_continuesUnauthenticated_whenUserIsUnknown() throws Exception {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN);
        when(jwtService.extractUsername(TOKEN)).thenReturn("ghost");
        when(userDetailsService.loadUserByUsername("ghost")).thenThrow(new UsernameNotFoundException("User 'ghost' not found"));

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("accepts the Bearer scheme regardless of its case, as RFC 7235 requires")
    void doFilterInternal_acceptsBearerScheme_regardlessOfCase() throws Exception {
        UserDetails userDetails = User.withUsername("admin").password("irrelevant").authorities("ROLE_ADMIN").build();
        request.addHeader(HttpHeaders.AUTHORIZATION, "bearer " + TOKEN);
        when(jwtService.extractUsername(TOKEN)).thenReturn("admin");
        when(userDetailsService.loadUserByUsername("admin")).thenReturn(userDetails);

        filter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("continues the chain unauthenticated when the account behind a valid token is disabled")
    void doFilterInternal_continuesUnauthenticated_whenAccountIsDisabled() throws Exception {
        UserDetails disabled = User.withUsername("admin").password("irrelevant").authorities("ROLE_ADMIN").disabled(true).build();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN);
        when(jwtService.extractUsername(TOKEN)).thenReturn("admin");
        when(userDetailsService.loadUserByUsername("admin")).thenReturn(disabled);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("lets an unexpected failure propagate instead of serving the request")
    void doFilterInternal_propagatesUnexpectedFailure_withoutRunningTheChain() {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN);
        when(jwtService.extractUsername(TOKEN)).thenThrow(new IllegalStateException("unexpected boom"));

        assertThrows(IllegalStateException.class, () -> filter.doFilterInternal(request, response, filterChain));

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(filterChain);
    }

    @Test
    @DisplayName("keeps an existing authentication and still continues the chain")
    void doFilterInternal_keepsExistingAuthentication_andContinuesTheChain() throws Exception {
        Authentication existing = UsernamePasswordAuthenticationToken.authenticated("someone-else", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(existing);
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN);

        filter.doFilterInternal(request, response, filterChain);

        assertSame(existing, SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService, userDetailsService);
    }
}
