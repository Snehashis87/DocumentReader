package com.snehashis.docmind.repository;

import com.snehashis.docmind.entity.DocumentMetadata;
import com.snehashis.docmind.entity.DocumentStatus;
import com.snehashis.docmind.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentMetadataRepo extends JpaRepository<DocumentMetadata, UUID> {

    List<DocumentMetadata> findAllByOrderByCreatedAtDesc();
    List<DocumentMetadata> findByStatus(DocumentStatus status);

    List<DocumentMetadata> findByUserOrderByCreatedAtDesc(User user);

    Optional<DocumentMetadata> findByIdAndUser(UUID id, User user);

    boolean existsByFilename(String filename);
}
