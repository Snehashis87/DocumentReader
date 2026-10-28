package com.snehashis.docmind.service;

import com.snehashis.docmind.config.AppProperties;
import com.snehashis.docmind.dto.*;
import com.snehashis.docmind.entity.Conversation;
import com.snehashis.docmind.entity.User;
import com.snehashis.docmind.repository.ConversationRepository;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RagService {

    private static final Logger logger = LoggerFactory.getLogger(RagService.class);
    private final VectorStore vectorStore;
    private final AppProperties appProperties;
    private final ChatClient chatClient;
    private final ConversationRepository conversationRepository;


    // The `askQuestion` method is responsible for processing a user's question in the context of a document retrieval system. It retrieves relevant document chunks based on the user's question, constructs a prompt for a chat model, and generates an answer. The method also logs the processing time and returns a structured response containing the answer, conversation ID, citations, and response time.
    public ChatResponseDto askQuestion(ChatRequestDto request, User user) {

        long startTime = System.currentTimeMillis();

        String conversationId = request.getConversationId() != null ? request.getConversationId() : UUID.randomUUID().toString();

        ensureConversationExists(conversationId, user, request.getQuestion());

        logger.info("Processing query: '{}' scoped docId: {}", request.getQuestion(), request.getDocumentId());

        List<Document> similarDocuments = this.retrieveRelevantDocuments(
                request.getQuestion(),
                request.getDocumentId(),
                request.getTopK(),
                request.getMinSimilarity(),
                user
        );

        // Here we are mapping the list of similar documents to a list of CitationDto objects. Each Document object is transformed into a CitationDto using the mapToCitation method. This allows us to create a structured representation of the relevant document chunks that can be used in the response.
        List<CitationDto> citationDtos = similarDocuments.stream()
                .map(this::mapToCitation)
                .toList();


        // We are building a context string from the similar documents. This context string will be used to provide additional information to the chat model, helping it generate a more informed response based on the relevant document chunks.
        String contextText = buildContextString(similarDocuments);


        // We are building a prompt for the chat model using the user's question and the context string. The buildPrompt method combines these elements into a single prompt that will be sent to the chat model for generating a response.
        String prompt = buildPrompt(request.getQuestion(), contextText);

        String answer = this.chatClient
                .prompt()
                .system(s -> s.param("doc_context", contextText))
                .user(request.getQuestion())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();

        long responseTime = System.currentTimeMillis() - startTime;
        logger.info("Completed Q&A in {} ms with {} citations", responseTime, citationDtos.size());
        return ChatResponseDto.builder()
                .answer(answer)
                .conversationId(conversationId)
                .citations(citationDtos)
                .responseTimeMs(responseTime)
                .build();

    }

    private void ensureConversationExists(String conversationId, User user, @NotBlank(message = "Question cannot be empty") String question) {

        conversationRepository.findByIdAndUser(conversationId, user)
                .orElseGet(() -> {
                    String title = question.length() > 50 ? question.substring(0, 47) + "....." : question;
                    return conversationRepository.save(
                            Conversation.builder().id(conversationId)
                                    .title(title)
                                    .user(user)
                                    .createdAt(LocalDateTime.now())
                                    .updatedAt(LocalDateTime.now())
                                    .build()
                    );
                });


    }


    // The `streamQuestionAnswer` method is responsible for processing a user's question in a streaming manner, allowing for real-time responses. It retrieves relevant document chunks based on the user's question, constructs a prompt for a chat model, and streams the generated answer back to the user. This method is particularly useful for applications that require immediate feedback or continuous interaction with the user.
    public Flux<String> streamQuestionAnswer(ChatRequestDto requestDto, User user) {
        logger.info("Streaming Q&A for query: '{}' scoped docId: {}", requestDto.getQuestion(), requestDto.getDocumentId());

        String conversationId = requestDto.getConversationId() != null
                ? requestDto.getConversationId()
                : UUID.randomUUID().toString();
        ensureConversationExists(conversationId, user, requestDto.getQuestion());

        // We are retrieving relevant documents based on the user's question, document ID, top K results, and minimum similarity threshold. This allows us to gather the most pertinent information to provide a comprehensive answer.
        List<Document> relevantDocuments = this.retrieveRelevantDocuments(
                requestDto.getQuestion(),
                requestDto.getDocumentId(),
                requestDto.getTopK(),
                requestDto.getMinSimilarity(),
                user
        );

        // We are building a context string from the relevant documents. This context string will be used to provide additional information to the chat model, helping it generate a more informed response based on the relevant document chunks.
        String contextText = buildContextString(relevantDocuments);


        // We are constructing a user prompt by combining the user's question with the context string. This prompt will be sent to the chat model to generate a response that takes into account both the user's query and the relevant document context.


//        String userPrompt = buildPrompt(requestDto.getQuestion(), contextText);


        // We are using the chat client to stream the response from the chat model based on the constructed user prompt. The stream() method allows us to receive the response in real-time, and we extract the content of the response to be sent back to the user.
        return chatClient.prompt()
                .system(s-> s.param("doc_context",contextText))
                .user(requestDto.getQuestion())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content()
                .concatWith(Flux.just("[DONE]"));
    }

    private String buildPrompt(@NotBlank(message = "Questions cannot be Empty") String question, String contextText) {
        if (contextText != null && !contextText.isBlank()) {
            return String.format("""
                    Document Context:
                   ---------------------
                   %s
                   ---------------------                                       
                   User Message / Question: %s            
                   Instructions:
                   - If the user's question relates to the document context above, prioritize answering using that context and reference key sections.
                   - If the user is asking a general question, greeting, or discussing topics beyond the document context, respond helpfully and conversationally using your general knowledge while weaving in relevant document context if applicable
                    
                    

            """, contextText, question);
        }else {
            return String.format("""
                    User Message / Question: %s            
                    Instructions:
                    - If the user's question relates to the document context above, prioritize answering using that context and reference key sections.
                    - If the user is asking a general question, greeting, or discussing topics beyond the document context, respond helpfully and conversationally using your general knowledge while weaving in relevant document context if applicable
            """, question);
        }

    }

    private String buildContextString(List<Document> similarDocuments) {
        if (similarDocuments == null || similarDocuments.isEmpty()) {
            return "";
        }
        return similarDocuments.stream()
                .map(doc->{
                    String fileName = (String) doc.getMetadata().getOrDefault("fileName", "Unknown File");
                    Object page = doc.getMetadata().getOrDefault("pageNumber", "N/A");
                    return String.format("[Source: %s | Page: %s]\n%s", fileName, page, doc.getText());
                }).collect(Collectors.joining("\n\n---\n\n"));
    }


    public SearchResultDto searchSimilarChunks(SearchRequestDto request, User user) {

        List<Document> matchedDocs = retrieveRelevantDocuments(
                request.getQuery(),
                request.getDocumentId(),
                request.getTopK(),
                request.getSimilaritySearch(),
                user

        );

        List<CitationDto> citations = matchedDocs.stream()
                .map(this::mapToCitation)
                .toList();

        return SearchResultDto.builder()
                .query(request.getQuery())
                .totalMatches(citations.size())
                .matches(citations)
                .build();
    }

    private CitationDto mapToCitation(Document document) {

        Map<String, Object> meta = document.getMetadata();
        UUID docId = null;
        if(meta.get("documentId") != null){
            try {
                docId = UUID.fromString(meta.get("documentId").toString());
            } catch (Exception ignore) {

            }
        }


        // What we are doing here is extracting the chunkIndex and pageNumber from the metadata of the document. We check if the values are instances of Number and convert them to Integer. If they are not present or not valid, we leave them as null. This allows us to handle cases where the metadata might not contain these fields, or they might be in an unexpected format.
        Integer chunkIndex = null;


        // instanceof -> This is a Java operator that checks if an object is an instance of a specific class or subclass. In this case, we are checking if the value associated with the "chunkIndex" key in the metadata map is an instance of the Number class. If it is, we can safely cast it to a Number and then convert it to an Integer.
        if(meta.get("chunkIndex") instanceof Number n){
            chunkIndex = n.intValue();
        }
        Integer pageNumber = null;
        if(meta.get("pageNumber") instanceof Number n){
            pageNumber = n.intValue();
        } else if (meta.get("page_number") instanceof Number n) {
            pageNumber = n.intValue();
        }

        Double score = null;
        if(meta.get("distance") instanceof Number n) {
            score = 1.0 - n.doubleValue();
        }


        return CitationDto.builder()
                .documentId(docId)
                .fileName((String) meta.getOrDefault("fileName", "Unknown"))
                .chunkIndex(chunkIndex)
                .pageNumber(pageNumber)
                .snippet(document.getText())
                .similarityScore(score)
                .metadata(meta)
                .build();
    }

    private List<Document> retrieveRelevantDocuments(@NotBlank(message = "Query cannot be blank") String query, UUID documentId, Integer topK, Double similaritySearch, User user) {
        int effectiveTopK = (topK != null && topK > 0) ? topK : appProperties.getRag().getTopK();

        double effectiveSimilarity = (similaritySearch != null) ? similaritySearch : appProperties.getRag().getSimilarityThreshold();

        SearchRequest.Builder searchRequestBuilder = SearchRequest.builder()
                .query(query)
                .topK(effectiveTopK);

        if (effectiveSimilarity > 0.0) {
            searchRequestBuilder.similarityThreshold(effectiveSimilarity);
        }

        FilterExpressionBuilder filterExpressionBuilder = new FilterExpressionBuilder();
        Filter.Expression filterExpression;

        if (documentId != null) {
            logger.info("Filtering from document :");
            filterExpression =
                    filterExpressionBuilder.and(
                            filterExpressionBuilder.eq("userId", user.getId().toString()),
                    filterExpressionBuilder.eq("documentId", documentId.toString())
                    ).build();
            searchRequestBuilder.filterExpression(filterExpressionBuilder.eq("documentId", documentId.toString()).build());
        }else {
            filterExpression = filterExpressionBuilder.eq("userId", user.getId().toString()).build();
        }
        searchRequestBuilder.filterExpression(filterExpression);

        try{
            List<Document> documents = vectorStore.similaritySearch(searchRequestBuilder.build());
            logger.info("Retrieved {} chunks for query: '{}' (scoped docId: {})", documents.size(), query, documentId);
            return documents;
        } catch (Exception e) {
            logger.error("Similarity search failed for query: '{}' (scoped docId: {}). Error: {}", query, documentId, e.getMessage());
            return Collections.emptyList();
        }

    }
}
