package com.snehashis.docmind.dto;

import lombok.*;

import java.util.List;


// What this class is for and what it does
// -> The `SearchResultDto` class is a Data Transfer Object (DTO) that represents the result of a search query in a document retrieval system. It contains information about the search query, the total number of matches found, and a list of `CitationDto` objects that represent the individual matches or citations related to the search query. This class is typically used to encapsulate and transfer search results between different layers of an application, such as from the service layer to the presentation layer.


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchResultDto {

    private String query;
    private int totalMatches;
    private List<CitationDto> matches;

}
