package com.example.Spotify.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

import jakarta.servlet.MultipartConfigElement;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class FileUploadConfig {

    private final FileStorageProperties fileStorageProperties;

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();

        // Set file size limits based on properties
        factory.setMaxFileSize(DataSize.parse("50MB"));
        factory.setMaxRequestSize(DataSize.parse("100MB"));

        // Set temp location for file processing
        factory.setLocation(System.getProperty("java.io.tmpdir"));

        log.info("Multipart configuration initialized with max file size: 50MB, max request size: 100MB");

        return factory.createMultipartConfig();
    }

    @Bean
    public MultipartResolver multipartResolver() {
        StandardServletMultipartResolver resolver = new StandardServletMultipartResolver();
        log.info("Multipart resolver configured");
        return resolver;
    }
}
