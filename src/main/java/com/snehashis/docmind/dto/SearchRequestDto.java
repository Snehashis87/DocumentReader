package com.snehashis.docmind.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;


// What this class is for and what it does
// -> The `SearchRequestDto` class is a Data Transfer Object (DTO) that represents a search request in a document retrieval system. It contains information about the search query, the specific document to search within (if applicable), the number of top results to return, and the similarity threshold for the search. This class is typically used to encapsulate and transfer search request data between different layers of an application, such as from the presentation layer to the service layer.


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SearchRequestDto {

    @NotBlank(message = "Query cannot be blank")
    private String query;

    private UUID documentId;

    private  Integer topK;

    private Double similaritySearch;
}
