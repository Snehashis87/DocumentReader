package com.snehashis.docmind.controller;


import com.snehashis.docmind.dto.ApiResponse;
import com.snehashis.docmind.dto.DocumentMetadataDto;
import com.snehashis.docmind.dto.DocumentResponseDto;
import com.snehashis.docmind.entity.User;
import com.snehashis.docmind.service.DocumentMetadataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.stylesheets.LinkStyle;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController()
@RequestMapping("/api/v1/documents")
@Tag(name = "Document Management", description = "Endpoints for managing documents in the DocMind application")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentMetadataService documentService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload and index a document(PDF, DOCX, TEXT, MD, CSV)", description = "This endpoint allows users to upload a document, which will then be indexed for future retrieval and analysis.")
    public ResponseEntity<ApiResponse<DocumentResponseDto>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) {

        log.info("Document uploading started by user");
        User user = (User) authentication.getPrincipal();
//        System.out.println("User uploading document: " + user.getUsername() + ", Email: " + user.getEmail() + ", id: " + user.getId());

        // Here, you would typically call a service to handle the file processing and indexing.
        DocumentResponseDto documentResponseDto = this.documentService.uploadAndProcessDocument(file, user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<DocumentResponseDto>builder()
                        .success(true)
                        .message("Document uploaded and indexed successfully.")
                        .data(documentResponseDto)
                        .timestamp(java.time.LocalDateTime.now())
                        .build());
    }


    // API to Upload multiple documents
    @PostMapping(value = "/upload-multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload and index multiple documents(PDF, DOCX, TEXT, MD, CSV)", description = "This endpoint allows users to upload multiple documents, which will then be indexed for future retrieval and analysis.")
    public ResponseEntity<ApiResponse<List<DocumentResponseDto>>> uploadMultipleDocuments(
            @RequestParam("files") List<MultipartFile> files,
            Authentication authenticate
    ) {
        User user = (User) authenticate.getPrincipal();

        // Here, uploadAndProcessMultipleDocuments is a method in the DocumentMetadataService class that handles the processing of multiple uploaded documents. It takes a list of MultipartFile objects (representing the uploaded files) as input and returns a list of DocumentResponseDto objects, which contain information about the processed documents.
        List<DocumentResponseDto> documentResponseDto = documentService.uploadAndProcessMultipleDocuments(files, user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ApiResponse.<List<DocumentResponseDto>>builder()
                                .success(true)
                                .message("Documents uploaded and indexed successfully.")
                                .data(documentResponseDto)
                                .timestamp(java.time.LocalDateTime.now())
                                .build());
    }

    //    list all uploaded documents of logged-in user
    @GetMapping("/user")
    @Operation(summary = "List all uploaded documents and their indexing status of the logged user")
    public ResponseEntity<ApiResponse<List<DocumentMetadataDto>>> getAllDocumentOfLoggedInUser(
            Authentication authentication
    ){
        User user=(User)authentication.getPrincipal();
        java.util.List<DocumentMetadataDto> documents =documentService.getAllDocumentsByUser(user);
        return ResponseEntity.ok(
                ApiResponse.<List<DocumentMetadataDto>>
                                builder()
                        .message("All documents is here")
                        .success(true)
                        .timestamp(LocalDateTime.now())
                        .data(documents)
                        .build()
        );
    }

    // API to List all uploaded documents

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    @Operation(summary = "List all uploaded documents", description = "This endpoint retrieves a list of all documents that have been uploaded and indexed in the system.")
    public ResponseEntity<ApiResponse<List<DocumentMetadataDto>>> getAllDocuments() {
        List<DocumentMetadataDto> documents = documentService.getAllDocuments();
        return ResponseEntity.ok(
                ApiResponse.<List<DocumentMetadataDto>>builder()
                        .success(true)
                        .message("Documents retrieved successfully.")
                        .data(documents)
                        .timestamp(java.time.LocalDateTime.now())
                        .build());
    }


    // API to get a specific document by ID
    @GetMapping("/{id}")
    @Operation(summary = "Get metadata of a document by ID", description = "This endpoint retrieves the metadata of a specific document based on its unique identifier (UUID).")
    public ResponseEntity<ApiResponse<DocumentMetadataDto>> getDocumentById(
            @PathVariable UUID id,
            Authentication authentication) {
        DocumentMetadataDto document = documentService.getDocumentById(id, (User) authentication.getPrincipal());
        return ResponseEntity.ok(
                ApiResponse.<DocumentMetadataDto>builder()
                        .success(true)
                        .message("Document retrieved successfully.")
                        .data(document)
                        .timestamp(java.time.LocalDateTime.now())
                        .build());
    }


    // API to delete a specific document by ID
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a document by ID", description = "This endpoint allows users to delete a specific document from the system based on its unique identifier (UUID).")
    public ResponseEntity<ApiResponse<Void>> deleteDocumentById(
            @PathVariable UUID id,
            Authentication authentication) {
        documentService.deleteDocumentById(id, (User) authentication.getPrincipal());
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Document deleted successfully.")
                        .data(null)
                        .timestamp(java.time.LocalDateTime.now())
                        .build());
    }

}
