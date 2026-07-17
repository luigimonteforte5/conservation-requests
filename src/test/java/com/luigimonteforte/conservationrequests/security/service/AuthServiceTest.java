package com.luigimonteforte.conservationrequests.security.service;

import com.luigimonteforte.conservationrequests.security.model.LoginRequest;
import com.luigimonteforte.conservationrequests.security.model.LoginResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    private static final LoginRequest LOGIN_REQUEST = new LoginRequest("admin", "s3cret");
    private static final String GENERIC_MESSAGE = "Invalid username or password";

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("performLogin returns the generated token when the authentication succeeds")
    void performLogin_returnsToken_whenAuthenticationSucceeds() {
        when(jwtService.generateToken("admin")).thenReturn("a.jwt.token");

        LoginResponse response = authService.performLogin(LOGIN_REQUEST);

        assertEquals(new LoginResponse("a.jwt.token"), response);
    }

    @Test
    @DisplayName("performLogin submits the received credentials to the AuthenticationManager")
    void performLogin_submitsReceivedCredentials_toAuthenticationManager() {
        authService.performLogin(LOGIN_REQUEST);

        ArgumentCaptor<UsernamePasswordAuthenticationToken> captor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(captor.capture());
        assertEquals("admin", captor.getValue().getPrincipal());
        assertEquals("s3cret", captor.getValue().getCredentials());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("authenticationFailures")
    @DisplayName("performLogin reports every authentication failure with the same generic message")
    void performLogin_reportsGenericMessage_forEveryAuthenticationFailure(AuthenticationException failure) {
        when(authenticationManager.authenticate(any())).thenThrow(failure);

        BadCredentialsException thrown =
                assertThrows(BadCredentialsException.class, () -> authService.performLogin(LOGIN_REQUEST));

        assertEquals(GENERIC_MESSAGE, thrown.getMessage());
    }

    static Stream<Arguments> authenticationFailures() {
        return Stream.of(
                Arguments.of(new BadCredentialsException("Bad credentials")),
                Arguments.of(new UsernameNotFoundException("User 'admin' not found")),
                Arguments.of(new DisabledException("User is disabled")),
                Arguments.of(new LockedException("User account is locked"))
        );
    }

    @Test
    @DisplayName("performLogin does not generate a token when the authentication fails")
    void performLogin_doesNotGenerateToken_whenAuthenticationFails() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.performLogin(LOGIN_REQUEST));

        verify(jwtService, never()).generateToken(any());
    }

    @Test
    @DisplayName("performLogin keeps the original failure as the cause for diagnostics")
    void performLogin_keepsOriginalFailureAsCause_whenAuthenticationFails() {
        AuthenticationException original = new DisabledException("User is disabled");
        when(authenticationManager.authenticate(any())).thenThrow(original);

        BadCredentialsException thrown =
                assertThrows(BadCredentialsException.class, () -> authService.performLogin(LOGIN_REQUEST));

        assertSame(original, thrown.getCause());
    }
}
