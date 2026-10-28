package com.snehashis.docmind.config;


import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


// The CorsConfig class is a configuration class that implements the WebMvcConfigurer interface to customize the CORS (Cross-Origin Resource Sharing) settings for the application. It uses the AppProperties class to retrieve the allowed origins and methods for CORS from the application properties.

// WebMvcConfigurer is an interface provided by Spring that allows developers to customize the configuration of Spring MVC. By implementing this interface, you can override its methods to modify the default behavior of Spring MVC, such as adding interceptors, formatters, view controllers, and in this case, configuring CORS settings.


@Configuration
@RequiredArgsConstructor
public class CorsConfig implements WebMvcConfigurer {
    private final AppProperties appProperties;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] allowedOrigins = appProperties.getCors().getAllowedOrigins().split(",");
        String[] allowedMethods = appProperties.getCors().getAllowedMethods().split(",");
        String[] allowedHeaders = appProperties.getCors().getAllowedHeaders().split(",");

        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods(allowedMethods)
                .allowedHeaders(allowedHeaders);
    }
}
