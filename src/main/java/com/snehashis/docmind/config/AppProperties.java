package com.snehashis.docmind.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
// ConfigurationProperties annotation is used to bind the properties defined in the application.properties or application.yml file to this class. The prefix "app" indicates that all properties starting with "app" will be mapped to this class.
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {

    private RagProperties rag = new RagProperties();
    private CorsProperties cors = new CorsProperties();

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CorsProperties{
        private String allowedOrigins = "*";
        private String allowedMethods = "GET,POST,PUT,DELETE,OPTIONS";
        private String allowedHeaders = "*";
    }
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RagProperties{
        // topK: The maximum number of relevant chunks to retrieve for a given query. This limits the number of chunks that will be considered when generating a response.
        private int topK = 5;

        // chunkSize: The maximum number of tokens (words or characters) in each chunk of text. This determines how large each chunk will be when the document is split into smaller pieces for processing.
        private int chunkSize = 600;


        //private int chunkOverlap = 100;

        // minChunkSizeChars: The minimum number of characters in each chunk of text. This ensures that chunks are not too small, which could lead to loss of context or meaning when processing the text.
        private int minChunkSizeChars = 350;

        // minChunkLengthToEmbed: The minimum length of a chunk that will be considered for embedding. Chunks shorter than this length will be ignored during the embedding process.
        private int minChunkLengthToEmbed = 5;

        // maxNumChunks: The maximum number of chunks that can be created from a single document. This limits the total number of chunks that will be generated during the chunking process.
        private int maxNumChunks = 10000;

        // similarityThreshold: The threshold for determining whether two chunks are considered similar. This is used to filter out chunks that are too similar to each other, which can help reduce redundancy in the retrieved chunks.
        private double similarityThreshold = 0.0;
    }
}
