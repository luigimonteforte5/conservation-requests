package com.luigimonteforte.conservationrequests.controller;

import com.luigimonteforte.conservationrequests.entity.Status;
import com.luigimonteforte.conservationrequests.model.CreateRequestDto;
import com.luigimonteforte.conservationrequests.model.RequestDto;
import com.luigimonteforte.conservationrequests.service.RequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/requests")
@RequiredArgsConstructor
public class RequestController {
    private final RequestService requestService;

    @PostMapping()
    public ResponseEntity<RequestDto> createRequest(@RequestBody @Valid CreateRequestDto createRequestDto) {
        RequestDto created = requestService.createRequest(createRequestDto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public ResponseEntity<PagedModel<RequestDto>> getRequests(
            @RequestParam(required = false) Long producerId,
            @RequestParam(required = false) Status status,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<RequestDto> page = requestService.getRequests(producerId, status, pageable);
        return ResponseEntity.ok(new PagedModel<>(page));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RequestDto> getRequest(@PathVariable Long id) {
        return ResponseEntity.ok(requestService.findById(id));
    }

    @PatchMapping("/{id}/validate")
    public ResponseEntity<RequestDto> validateRequest(@PathVariable Long id) {
        return ResponseEntity.ok(requestService.changeStatus(id, Status.VALIDATED));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<RequestDto> rejectRequest(@PathVariable Long id) {
        return ResponseEntity.ok(requestService.changeStatus(id, Status.REJECTED));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<RequestDto> completeRequest(@PathVariable Long id) {
        return ResponseEntity.ok(requestService.changeStatus(id, Status.COMPLETED));
    }
}
