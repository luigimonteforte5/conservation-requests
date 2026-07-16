package com.luigimonteforte.conservationrequests.model;

import com.luigimonteforte.conservationrequests.entity.Document;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

import java.time.Instant;

/**
 * DTO for {@link Document}
 */
public record DocumentDto(Long id, @NotBlank String fileName, @NotBlank String mimeType, @Positive Long fileSize,
                          @NotBlank String hash,
                          @PastOrPresent Instant documentDate) {
}