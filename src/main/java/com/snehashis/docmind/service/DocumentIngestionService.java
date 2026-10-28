package com.snehashis.docmind.service;


import com.snehashis.docmind.config.AppProperties;
import com.snehashis.docmind.entity.DocumentMetadata;
import com.snehashis.docmind.entity.DocumentStatus;
import com.snehashis.docmind.exception.DocumentProcessingException;
import com.snehashis.docmind.repository.DocumentMetadataRepo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DocumentIngestionService {


    private static final Logger logger = LoggerFactory.getLogger(DocumentIngestionService.class);

    private final VectorStore vectorStore;

    private final DocumentMetadataRepo documentMetadataRepo;

    private final AppProperties appProperties;



    // Ingestion is the process of taking the parsed documents and storing them in a database or any other storage system for further processing or retrieval. This service will handle that process.
    public int ingest(DocumentMetadata documentMetadata, List<Document> parsedDocuments) {

        logger.info("Ingesting document: {} with {} parsed documents and id {}", documentMetadata.getFilename(), parsedDocuments.size(), documentMetadata.getId());

        try {
            documentMetadata.setStatus(DocumentStatus.PROCESSING);
            documentMetadata.setTotalPages(parsedDocuments.size());

            // 1. Text chunking: The parsed documents are divided into smaller chunks of text. This is done to make it easier to process and analyze the text, especially for large documents. Each chunk can be processed independently, which can improve performance and scalability.

            TokenTextSplitter textSplitter = TokenTextSplitter.builder()
                    .withChunkSize(appProperties.getRag().getChunkSize())
                    .withMinChunkSizeChars(appProperties.getRag().getMinChunkSizeChars())
                    .withMinChunkLengthToEmbed(appProperties.getRag().getMinChunkLengthToEmbed())
                    .withMaxNumChunks(appProperties.getRag().getMaxNumChunks())

                    // withKeepSeparator(true) is a configuration option for the TokenTextSplitter that determines whether to keep the separator (e.g., whitespace, punctuation) between chunks when splitting the text. When set to true, the separator will be included at the end of each chunk, which can help maintain context and improve the quality of embeddings generated from the chunks. This is particularly useful for natural language processing tasks where preserving context is important for understanding the meaning of the text.
                    .withKeepSeparator(true)
                    .build();

            List<Document> chunks = textSplitter.apply(parsedDocuments);
            if (chunks.isEmpty()) {
                documentMetadata.setStatus(DocumentStatus.FAILED);
                documentMetadata.setErrorMessage("No chunks were created from the document. Please check the document content and try again.");
                documentMetadataRepo.save(documentMetadata);
                return 0;
            }

            // 2. Metadata enrichment: Each chunk is enriched with metadata, such as the document ID, page number, and any other relevant information. This metadata can be used later for searching and filtering the chunks.
            List<Document> enrichedChunks = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                Document chunk = chunks.get(i);
                Map<String, Object> enrichedMetadata = new HashMap<>(chunk.getMetadata());
                enrichedMetadata.put("documentId", documentMetadata.getId().toString());
                enrichedMetadata.put("fileName", documentMetadata.getFilename());
                enrichedMetadata.put("contentType", documentMetadata.getContentType());
                enrichedMetadata.put("chunkIndex", i);

                // These metadata fields are important for user-based document retrieval and management. By including the userId in the metadata, we can easily associate each chunk with the user who uploaded the document. This is crucial for implementing access control, personalized search, and other user-specific features in the application.


//                enrichedMetadata.put("userId", documentMetadata.getUser().getId());


               // this step is very important for user based document
                enrichedMetadata.put("userId", documentMetadata.getUser().getId().toString());

                // Preserve or calculate page number if available
                Object pageNumber = chunk.getMetadata().get("page_number");

                if (pageNumber == null) {
                    pageNumber = chunk.getMetadata().get("pageNumber");

                }

                if (pageNumber != null) {
                    enrichedMetadata.put("pageNumber", pageNumber);
                }

                Document enrichedDoc = new Document(chunk.getText(), enrichedMetadata);
                enrichedChunks.add(enrichedDoc);

            }


//            3. write chunks and embedding to pg vector
            logger.info("Writing {} vector chunks to PgVectorStore for document: {}", enrichedChunks.size(), documentMetadata.getFilename());
            vectorStore.add(enrichedChunks);


            // 4. Update document status to INDEXED after successful ingestion
            documentMetadata.setStatus(DocumentStatus.INDEXED);
            documentMetadata.setTotalChunks(enrichedChunks.size());
            documentMetadata.setErrorMessage(null); // Clear any previous error messages
            documentMetadataRepo.save(documentMetadata);
            logger.info("Document ingestion completed successfully for document: {} with id {}", documentMetadata.getFilename(), documentMetadata.getId());
            return enrichedChunks.size();

        }catch(Exception e){
            logger.error("Error during document ingestion for document: {} with id {}. Error: {}", documentMetadata.getFilename(), documentMetadata.getId(), e.getMessage());
            documentMetadata.setStatus(DocumentStatus.FAILED);
            documentMetadata.setErrorMessage("Error during ingestion: " + e.getMessage());
            documentMetadataRepo.save(documentMetadata);
            throw new DocumentProcessingException("Error during document ingestion for document: " + documentMetadata.getFilename() + " with id " + documentMetadata.getId(), e);

        }
    }
}
