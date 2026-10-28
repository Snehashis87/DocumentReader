package com.snehashis.docmind.controller;


import com.snehashis.docmind.dto.*;
import com.snehashis.docmind.entity.User;
import com.snehashis.docmind.service.RagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/chat")
@Tag(
        name = "Chat Management",
        description = "Endpoints for managing chat interactions in the DocMind application"
)
@RequiredArgsConstructor
public class ChatController {

    private final RagService ragService;

    // API to ask a question and get an answer from the RAG system
    @PostMapping("/query")
    @Operation(
            summary = "Ask a question and get an answer from the RAG system",
            description = "This endpoint allows users to ask a question, which will be processed by the RAG system to provide an answer based on the indexed documents."
    )
    public ResponseEntity<ApiResponse<ChatResponseDto>> askQuestion(
            // The @Valid annotation is used to indicate that the chatRequestDto object should be validated against the constraints defined in its class (e.g., @NotBlank for the question field). If the validation fails, Spring will automatically return a 400 Bad Request response with details about the validation errors.

            // The @RequestBody annotation is used to bind the incoming JSON request body to the chatRequestDto parameter. This means that when a client sends a POST request to this endpoint with a JSON payload, Spring will automatically deserialize the JSON into a ChatRequestDto object.
            @Valid @RequestBody ChatRequestDto chatRequestDto,
            Authentication authentication
    ){
        User user = (User) authentication.getPrincipal();
        ChatResponseDto chatResponseDto = ragService.askQuestion(chatRequestDto, user);
        return ResponseEntity.ok(
                ApiResponse.<ChatResponseDto>builder()
                        .success(true)
                        .message("Question answered successfully.")
                        .data(chatResponseDto)
                        .timestamp(java.time.LocalDateTime.now())
                        .build()
        );
    }

    @PostMapping(value="/stream", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            summary = "Stream question and answer from the RAG system",
            description = "This endpoint allows users to ask a question, which will be processed by the RAG system to provide an answer based on the indexed documents in a streaming manner."
    )
    public Flux<String> streamQuestion(
            @Valid @RequestBody ChatRequestDto chatRequestDto,
            Authentication authentication
    ){
        User user = (User) authentication.getPrincipal();
        return ragService.streamQuestionAnswer(chatRequestDto, user);
    }

    @PostMapping("/search/similarity")
    @Operation(
            summary = "Search for similar documents based on a question",
            description = "This endpoint allows users to search for documents that are similar to the provided question, based on the indexed documents in the RAG system."
    )
    public ResponseEntity<ApiResponse<SearchResultDto>> searchSimilar(
            @Valid @RequestBody SearchRequestDto searchRequestDto,
            Authentication authentication
    ){
        User user = (User) authentication.getPrincipal();
        SearchResultDto searchResultDto = ragService.searchSimilarChunks(searchRequestDto, user);
        return ResponseEntity.ok(
                ApiResponse.<SearchResultDto>builder()
                        .success(true)
                        .message("Similar documents retrieved successfully.")
                        .data(searchResultDto)
                        .timestamp(java.time.LocalDateTime.now())
                        .build()
        );
    }

}
