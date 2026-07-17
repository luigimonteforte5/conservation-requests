package com.luigimonteforte.conservationrequests.security.service;

import com.luigimonteforte.conservationrequests.security.model.LoginRequest;
import com.luigimonteforte.conservationrequests.security.model.LoginResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public LoginResponse performLogin(LoginRequest loginRequest) {
        String username = loginRequest.username();
        try{
            authenticationManager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(username, loginRequest.password()));
        } catch (AuthenticationException ae) {
            log.warn("Failed login attempt for username '{}': {}", username, ae.getMessage());
            throw new BadCredentialsException("Invalid username or password", ae);
        }
        String token = jwtService.generateToken(username);
        log.info("Successful login for username '{}'", username);
        return new LoginResponse(token);
    }
}
