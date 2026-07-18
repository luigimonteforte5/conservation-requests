package com.luigimonteforte.conservationrequests.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.luigimonteforte.conservationrequests.entity.Status;
import com.luigimonteforte.conservationrequests.exception.DuplicateRequestException;
import com.luigimonteforte.conservationrequests.exception.InvalidStateTransitionException;
import com.luigimonteforte.conservationrequests.exception.ResourceNotFoundException;
import com.luigimonteforte.conservationrequests.model.CreateRequestDto;
import com.luigimonteforte.conservationrequests.model.DocumentDto;
import com.luigimonteforte.conservationrequests.model.RequestDto;
import com.luigimonteforte.conservationrequests.service.RequestService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.hamcrest.Matchers.endsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RequestController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("RequestController")
class RequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RequestService requestService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private static DocumentDto sampleDocumentDto() {
        return new DocumentDto(null, "file.pdf", "application/pdf", 1024L, "hash", Instant.parse("2024-01-01T00:00:00Z"));
    }

    private static RequestDto sampleRequestDto(Long id, Status status) {
        return new RequestDto(id, 100L, 1L, "INVOICE", status, Instant.parse("2024-01-01T00:00:00Z"), Instant.parse("2024-01-01T00:00:00Z"), List.of(sampleDocumentDto()));
    }

    @Test
    @DisplayName("POST /api/v1/requests returns 201 with Location header when the request is valid")
    void createRequest_returns201WithLocation_whenValid() throws Exception {
        CreateRequestDto createRequestDto = new CreateRequestDto(1L, "INVOICE", 100L, List.of(sampleDocumentDto()));
        RequestDto created = sampleRequestDto(10L, Status.RECEIVED);
        when(requestService.createRequest(any(CreateRequestDto.class))).thenReturn(created);

        mockMvc.perform(post("/api/v1/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequestDto)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/v1/requests/10")))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("RECEIVED"));

        ArgumentCaptor<CreateRequestDto> captor = ArgumentCaptor.forClass(CreateRequestDto.class);
        verify(requestService).createRequest(captor.capture());
        assertEquals(createRequestDto, captor.getValue());
    }

    @Test
    @DisplayName("POST /api/v1/requests returns 400 and does not call the service when required fields are missing")
    void createRequest_returns400_whenInvalid() throws Exception {
        CreateRequestDto invalidDto = new CreateRequestDto(null, "", null, List.of());

        mockMvc.perform(post("/api/v1/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());

        verify(requestService, never()).createRequest(any());
    }

    @Test
    @DisplayName("POST /api/v1/requests validates each document, not just the list of documents")
    void createRequest_returns400_whenADocumentIsInvalid() throws Exception {
        DocumentDto invalidDocument = new DocumentDto(null, "  ", "", -1L, "",
                Instant.now().plus(1, ChronoUnit.DAYS));
        CreateRequestDto createRequestDto = new CreateRequestDto(1L, "INVOICE", 100L, List.of(invalidDocument));

        mockMvc.perform(post("/api/v1/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequestDto)))
                .andExpect(status().isBadRequest())
                // the violations must be reported per element, which only happens when
                // @Valid cascades to the type argument rather than to the list itself
                .andExpect(jsonPath("$.errors['documents[0].fileName']").exists())
                .andExpect(jsonPath("$.errors['documents[0].mimeType']").exists())
                .andExpect(jsonPath("$.errors['documents[0].fileSize']").exists())
                .andExpect(jsonPath("$.errors['documents[0].hash']").exists())
                .andExpect(jsonPath("$.errors['documents[0].documentDate']").exists());

        verify(requestService, never()).createRequest(any());
    }

    @Test
    @DisplayName("POST /api/v1/requests returns 409 when the request is a duplicate")
    void createRequest_returns409_whenDuplicate() throws Exception {
        CreateRequestDto createRequestDto = new CreateRequestDto(1L, "INVOICE", 100L, List.of(sampleDocumentDto()));
        when(requestService.createRequest(any(CreateRequestDto.class)))
                .thenThrow(new DuplicateRequestException("Request with externalId 100 and producerId 1 already exists"));

        mockMvc.perform(post("/api/v1/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequestDto)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("GET /api/v1/requests/{id} returns 200 with the request when found")
    void getRequest_returns200_whenFound() throws Exception {
        when(requestService.findById(10L)).thenReturn(sampleRequestDto(10L, Status.RECEIVED));

        mockMvc.perform(get("/api/v1/requests/{id}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.producerId").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/requests/{id} returns 404 when not found")
    void getRequest_returns404_whenMissing() throws Exception {
        when(requestService.findById(99L)).thenThrow(new ResourceNotFoundException("No Resource found with id 99"));

        mockMvc.perform(get("/api/v1/requests/{id}", 99L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/requests forwards producerId and status filters and returns the paginated result")
    void getRequests_returnsPagedResult_withFilters() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        Page<RequestDto> page = new PageImpl<>(List.of(sampleRequestDto(10L, Status.RECEIVED)), pageable, 1);
        when(requestService.getRequests(eq(1L), eq(Status.RECEIVED), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/requests")
                        .param("producerId", "1")
                        .param("status", "RECEIVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.page.totalElements").value(1));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(requestService).getRequests(eq(1L), eq(Status.RECEIVED), pageableCaptor.capture());
        assertEquals(0, pageableCaptor.getValue().getPageNumber());
        assertEquals(10, pageableCaptor.getValue().getPageSize());
    }

    @Test
    @DisplayName("GET /api/v1/requests returns 400 for an unparseable status filter")
    void getRequests_returns400_whenStatusIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/requests").param("status", "NOT_A_STATUS"))
                .andExpect(status().isBadRequest());

        verify(requestService, never()).getRequests(any(), any(), any());
    }

    @Test
    @DisplayName("GET /api/v1/requests works without filters")
    void getRequests_returnsPagedResult_withoutFilters() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        Page<RequestDto> page = new PageImpl<>(List.of(), pageable, 0);
        when(requestService.getRequests(isNull(), isNull(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    @DisplayName("GET /api/v1/requests exposes pagination metadata under 'page', not as internals of PageImpl")
    void getRequests_serializesPaginationMetadata_asAStableContract() throws Exception {
        Pageable pageable = PageRequest.of(1, 5);
        Page<RequestDto> page = new PageImpl<>(List.of(sampleRequestDto(10L, Status.RECEIVED)), pageable, 11);
        when(requestService.getRequests(isNull(), isNull(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/requests").param("page", "1").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.page.size").value(5))
                .andExpect(jsonPath("$.page.number").value(1))
                .andExpect(jsonPath("$.page.totalElements").value(11))
                .andExpect(jsonPath("$.page.totalPages").value(3))
                // the internals of PageImpl and Sort must not leak into the payload
                .andExpect(jsonPath("$.pageable").doesNotExist())
                .andExpect(jsonPath("$.sort").doesNotExist())
                .andExpect(jsonPath("$.totalElements").doesNotExist())
                .andExpect(jsonPath("$.numberOfElements").doesNotExist())
                .andExpect(jsonPath("$.empty").doesNotExist());
    }

    @Test
    @DisplayName("PATCH /api/v1/requests/{id}/validate transitions the request to VALIDATED")
    void validateRequest_returns200_withValidatedStatus() throws Exception {
        when(requestService.changeStatus(10L, Status.VALIDATED)).thenReturn(sampleRequestDto(10L, Status.VALIDATED));

        mockMvc.perform(patch("/api/v1/requests/{id}/validate", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VALIDATED"));
    }

    @Test
    @DisplayName("PATCH /api/v1/requests/{id}/reject transitions the request to REJECTED")
    void rejectRequest_returns200_withRejectedStatus() throws Exception {
        when(requestService.changeStatus(10L, Status.REJECTED)).thenReturn(sampleRequestDto(10L, Status.REJECTED));

        mockMvc.perform(patch("/api/v1/requests/{id}/reject", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    @DisplayName("PATCH /api/v1/requests/{id}/complete transitions the request to COMPLETED")
    void completeRequest_returns200_withCompletedStatus() throws Exception {
        when(requestService.changeStatus(10L, Status.COMPLETED)).thenReturn(sampleRequestDto(10L, Status.COMPLETED));

        mockMvc.perform(patch("/api/v1/requests/{id}/complete", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("PATCH /api/v1/requests/{id}/validate returns 409 for an illegal transition")
    void validateRequest_returns409_whenTransitionIsIllegal() throws Exception {
        when(requestService.changeStatus(10L, Status.VALIDATED))
                .thenThrow(new InvalidStateTransitionException("Cannot transition request 10 from COMPLETED to VALIDATED"));

        mockMvc.perform(patch("/api/v1/requests/{id}/validate", 10L))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PATCH /api/v1/requests/{id}/validate returns 404 when the request does not exist")
    void validateRequest_returns404_whenMissing() throws Exception {
        when(requestService.changeStatus(99L, Status.VALIDATED))
                .thenThrow(new ResourceNotFoundException("No Resource found with id 99"));

        mockMvc.perform(patch("/api/v1/requests/{id}/validate", 99L))
                .andExpect(status().isNotFound());
    }
}
