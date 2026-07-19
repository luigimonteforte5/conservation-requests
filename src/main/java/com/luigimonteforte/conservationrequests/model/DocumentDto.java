package com.luigimonteforte.conservationrequests.model;

import com.luigimonteforte.conservationrequests.entity.Document;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

import java.time.Instant;

/**
 * DTO for {@link Document}
 */
public record DocumentDto(
        @Schema(description = "Assigned by the service; ignored on creation", example = "128",
                accessMode = Schema.AccessMode.READ_ONLY) Long id,
        @NotBlank @Schema(description = "Name of the conserved file", example = "invoice-2026-0001.pdf") String fileName,
        @NotBlank @Schema(description = "Media type of the file", example = "application/pdf") String mimeType,
        @Positive @Schema(description = "Size of the file in bytes", example = "20480") Long fileSize,
        @NotBlank @Schema(description = "Digest of the file content, as computed by the producer",
                example = "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08") String hash,
        @PastOrPresent @Schema(description = "Date the document carries; cannot be in the future",
                example = "2026-07-18T00:00:00Z") Instant documentDate) {
}
