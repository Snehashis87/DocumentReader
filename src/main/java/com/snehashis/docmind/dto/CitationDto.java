package com.snehashis.docmind.dto;

import lombok.*;

import java.util.Map;
import java.util.UUID;


// What these class is for what it does
// -> The `CitationDto` class is a Data Transfer Object (DTO) that represents a citation or reference to a specific chunk of text from a document. It contains information about the document, the specific chunk, and its relevance to a search query. This class is typically used in applications that involve document processing, search, and retrieval, such as in a RAG (Retrieval-Augmented Generation) system.

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CitationDto {

    private UUID documentId;
    private String fileName;
    private Integer chunkIndex;
    private Integer pageNumber;
    private String snippet;
    private Double similarityScore;
    private Map<String, Object> metadata;

}
