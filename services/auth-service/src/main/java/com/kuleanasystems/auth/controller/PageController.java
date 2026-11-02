package com.kuleanasystems.auth.controller;

import com.kuleanasystems.auth.dto.PageCreateRequest;
import com.kuleanasystems.auth.dto.PageResponse;
import com.kuleanasystems.auth.entity.Page;
import com.kuleanasystems.auth.repository.PageRepository;
import com.kuleanasystems.auth.spec.PageSpecification;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/pages")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class PageController {

    private final PageRepository pageRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'VIEWER', 'ADMIN', 'OWNER', 'ASSET-IT')")
    public ResponseEntity<List<PageResponse>> getPages(
            @RequestParam(required = false) UUID id,
            @RequestParam(required = false) String path,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) String updatedBy) {

        Specification<Page> spec = Specification.where(PageSpecification.hasId(id))
                .and(PageSpecification.hasPath(path))
                .and(PageSpecification.hasName(name))
                .and(PageSpecification.hasDescription(description))
                .and(PageSpecification.hasCreatedBy(createdBy))
                .and(PageSpecification.hasUpdatedBy(updatedBy));

        List<Page> pages = pageRepository.findAll(spec);

        if (pages.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        List<PageResponse> response = pages.stream()
                .map(this::mapToPageResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'OWNER', 'ADMIN', 'ASSET-IT')")
    public ResponseEntity<?> createPage(@RequestBody PageCreateRequest request) {
        if (request.getPath() == null || request.getName() == null) {
            return ResponseEntity.badRequest().body("Path and Name are required.");
        }

        Specification<Page> pathSpec = PageSpecification.hasPath(request.getPath());
        if (!pageRepository.findAll(pathSpec).isEmpty()) {
            return ResponseEntity.badRequest().body("Page with path '" + request.getPath() + "' already exists.");
        }

        Page page = Page.builder()
                .path(request.getPath())
                .name(request.getName())
                .description(request.getDescription())
                .build();

        Page savedPage = pageRepository.save(page);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedPage.getId())
                .toUri();

        return ResponseEntity.created(location).body(mapToPageResponse(savedPage));
    }

    private PageResponse mapToPageResponse(Page page) {
        return PageResponse.builder()
                .id(page.getId())
                .path(page.getPath())
                .name(page.getName())
                .description(page.getDescription())
                .createdBy(page.getCreatedBy())
                .createdAt(page.getCreatedAt())
                .updatedBy(page.getUpdatedBy())
                .updatedAt(page.getUpdatedAt())
                .build();
    }
}
