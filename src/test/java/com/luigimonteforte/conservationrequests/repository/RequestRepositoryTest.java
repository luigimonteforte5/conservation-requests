package com.luigimonteforte.conservationrequests.repository;

import com.luigimonteforte.conservationrequests.entity.Document;
import com.luigimonteforte.conservationrequests.entity.Request;
import com.luigimonteforte.conservationrequests.entity.Status;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("RequestRepository")
class RequestRepositoryTest {

    @Autowired
    private RequestRepository requestRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("saving a request cascades and persists its documents")
    void save_persistsAttachedDocuments() {
        Document document = Document.builder()
                .fileName("file.pdf")
                .mimeType("application/pdf")
                .fileSize(1024L)
                .hash("hash")
                .documentDate(Instant.parse("2024-01-01T00:00:00Z"))
                .build();
        Request request = Request.builder()
                .externalId(100L)
                .producerId(1L)
                .documentType("INVOICE")
                .status(Status.RECEIVED)
                .documents(List.of(document))
                .build();
        document.setRequest(request);

        Request saved = requestRepository.save(request);
        entityManager.flush();
        entityManager.clear();

        Request reloaded = requestRepository.findById(saved.getId()).orElseThrow();
        assertEquals(1, reloaded.getDocuments().size());
        assertEquals("file.pdf", reloaded.getDocuments().getFirst().getFileName());
    }
}
