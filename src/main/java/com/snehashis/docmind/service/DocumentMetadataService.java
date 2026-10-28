package com.snehashis.docmind.service;

import com.snehashis.docmind.dto.DocumentMetadataDto;
import com.snehashis.docmind.dto.DocumentResponseDto;
import com.snehashis.docmind.entity.DocumentMetadata;
import com.snehashis.docmind.entity.DocumentStatus;
import com.snehashis.docmind.entity.User;
import com.snehashis.docmind.exception.DocumentProcessingException;
import com.snehashis.docmind.exception.ResourceNotFoundException;
import com.snehashis.docmind.repository.DocumentMetadataRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class DocumentMetadataService {

    // Logger is used for logging messages, which can be helpful for debugging and monitoring the application's behavior.
    private static final Logger logger = LoggerFactory.getLogger(DocumentMetadataService.class);


    private final DocumentMetadataRepo documentMetadataRepo;

    private final DocumentParserService documentParserService;

    private final DocumentIngestionService documentIngestionService;

    private final ModelMapper modelMapper;

    // JdbcTemplate is used for executing SQL queries directly against the database. It provides a convenient way to interact with the database without the need for complex ORM frameworks.
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public DocumentResponseDto uploadAndProcessDocument(MultipartFile file, User user) {

        // what happens in the below line is that we are getting the original filename of the uploaded file. If the original filename is not available (i.e., it is null), we assign a default name "Document" to the fileName variable. This ensures that we always have a valid name for the document, even if the user did not provide one.
        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "Document";

        if (documentMetadataRepo.existsByFilename(fileName)) {
            throw new IllegalArgumentException(
                    "A document named '" + fileName + "' already exists. Delete the existing document or rename the file before uploading."
            );
        }

        // The content type of the uploaded file is determined using the getContentType() method of the MultipartFile class. If the content type is not available (i.e., it is null), we assign a default content type of "application/octet-stream" to the contentType variable. This default content type is a generic binary stream, which can be used for files that do not have a specific MIME type.

        // application/octet-stream is a generic MIME type that indicates that the file is a binary stream. It is often used as a fallback for files that do not have a specific MIME type or when the content type cannot be determined. In this case, if the uploaded file does not have a content type, we use "application/octet-stream" as a default to ensure that the file can still be processed and stored correctly.
        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";


        // Here, we are creating a new instance of the DocumentMetadata class using the builder pattern. The builder pattern allows us to construct complex objects step by step, providing a more readable and maintainable way to create instances of classes with many attributes.

        // We are Storing the user information in the document metadata. This is important for associating the uploaded document with the user who uploaded it, allowing for user-specific document management and access control.
        DocumentMetadata documentMetadata = DocumentMetadata.builder()
                .filename(fileName)
                .contentType(contentType)
                .fileSize(file.getSize())
                .status(DocumentStatus.UPLOADING)
                .createdAt(LocalDateTime.now())
                .user(user)
                .build();

        // Save the document metadata to the database using the documentMetadataRepo. The save() method persists the documentMetadata object to the database and returns the saved instance, which may include additional information such as the generated ID.
        documentMetadata = documentMetadataRepo.saveAndFlush(documentMetadata);

        List<Document> parsedDocuments = null;
        int chunksCreated = 0;

        try {
            // parse the file using the documentParserService. The parse() method takes the uploaded file as input and returns a list of Document objects, which represent the parsed content of the document. This step is crucial for extracting meaningful information from the uploaded file, such as text, images, or other relevant data.
            parsedDocuments = documentParserService.parse(file);

            // ingest service is responsible for taking the parsed documents and storing them in a way that allows for efficient retrieval and analysis. The ingest() method takes the document metadata and the list of parsed documents as input, and it returns the number of chunks created during the ingestion process. This step is essential for preparing the document data for future search and retrieval operations.
            chunksCreated = documentIngestionService.ingest(documentMetadata, parsedDocuments);

        }catch (DocumentProcessingException e){
            logger.error("Document processing failed for document ID {}: {}", documentMetadata.getId(), e.getMessage());
            jdbcTemplate.update(
                    "DELETE FROM docmind_vectors WHERE metadata->>'documentId' = ?",
                    documentMetadata.getId().toString()
            );
            documentMetadataRepo.delete(documentMetadata);
            throw e;
        }


        return DocumentResponseDto.builder()
                .id(documentMetadata.getId())
                .fileName(documentMetadata.getFilename())
                .fileSize(documentMetadata.getFileSize())
                .status(documentMetadata.getStatus())
                .chunksCreated(chunksCreated)
                .message("Document uploaded and processed successfully.")
                .userId(user.getId())
                .build();
//        return null;
    }

    public List<DocumentResponseDto> uploadAndProcessMultipleDocuments(List<MultipartFile> files, User user) {
        // Here, we are creating an empty list called documentResponseDto to store the results of processing each uploaded document. We then iterate over the list of uploaded files (files) using a for-each loop. For each file, we call the uploadAndProcessDocument method to handle the upload and processing of that specific document. The result of processing each document is stored in a DocumentResponseDto object, which is then added to the documentResponseDto list. Finally, we return the list of DocumentResponseDto objects, which contains the results for all uploaded documents.
        List<DocumentResponseDto> documentResponseDto = new ArrayList<>();
        for (MultipartFile file : files) {
            DocumentResponseDto result = this.uploadAndProcessDocument(file, user);
            documentResponseDto.add(result);
        }
        return documentResponseDto;
    }

    public List<DocumentMetadataDto> getAllDocuments() {

        List<DocumentMetadata> allDocuments= documentMetadataRepo.findAllByOrderByCreatedAtDesc();

       return allDocuments.stream()
                .map(documentMetadata -> modelMapper.map(documentMetadata, DocumentMetadataDto.class))
                .toList();

    }

    public List<DocumentMetadataDto> getAllDocumentsByUser(User user) {
        List<DocumentMetadata> documents = documentMetadataRepo.findByUserOrderByCreatedAtDesc(user);
        return documents.stream()
                .map(documentMetadata -> modelMapper.map(documentMetadata, DocumentMetadataDto.class))
                .toList();
    }


    public DocumentMetadataDto getDocumentById(UUID id, User user) {

        DocumentMetadata document = documentMetadataRepo.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Document with ID " + id + " not found"));
        return modelMapper.map(document, DocumentMetadataDto.class);
    }

    @Transactional
    public void deleteDocumentById(UUID id, User user) {
        documentMetadataRepo.findByIdAndUser(id, user)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Document with ID " + id + " not found"));

        String deleteVectorEntriesSql =
                "DELETE FROM docmind_vectors WHERE metadata->>'documentId' = ?";

        int deleteCount = jdbcTemplate.update(deleteVectorEntriesSql, id.toString());

        logger.info("Deleted {} vector entries for document ID {}", deleteCount, id);

        documentMetadataRepo.deleteById(id);
    }
}
