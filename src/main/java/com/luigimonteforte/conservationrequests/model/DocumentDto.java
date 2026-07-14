package com.luigimonteforte.conservationrequests.model;

import com.luigimonteforte.conservationrequests.entity.Document;

import java.time.Instant;

/**
 * DTO for {@link Document}
 */
public record DocumentDto(Long id, String fileName, String mimeType, Long fileSize, String hash, Instant documentDate) {
}