package com.luigimonteforte.conservationrequests.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.security")
public record AppSecurityProperties(
                                    @NotBlank @Size(min = 32, message = "JWT secret must be at least 32 characters (256 bit) long") String jwtSecret,
                                    @NotNull Duration jwtExpiration, @NotNull String adminUsername,
                                    @NotNull @Pattern(regexp = "^\\$2[aby]\\$\\d{2}\\$[./0-9A-Za-z]{53}$", message = "Admin password must be a valid bcrypt hash") String adminPassword) {
}
