package com.example.Spotify.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileStorageInitializer implements CommandLineRunner {

    private final FileStorageProperties fileStorageProperties;

    @Override
    public void run(String... args) {
        try {
            createDirectoryIfNotExists(fileStorageProperties.getBaseDir());
            createDirectoryIfNotExists(fileStorageProperties.getSongsDir());
            createDirectoryIfNotExists(fileStorageProperties.getCoversDir());
            createDirectoryIfNotExists(fileStorageProperties.getAlbumsDir());

            log.info("File storage directories initialized successfully");
            log.info("Base directory: {}", fileStorageProperties.getBaseDir());
            log.info("Songs directory: {}", fileStorageProperties.getSongsDir());
            log.info("Covers directory: {}", fileStorageProperties.getCoversDir());
            log.info("Albums directory: {}", fileStorageProperties.getAlbumsDir());

        } catch (Exception e) {
            log.error("Failed to initialize file storage directories", e);
            throw new RuntimeException("File storage initialization failed", e);
        }
    }

    private void createDirectoryIfNotExists(String directory) {
        try {
            if (directory != null) {
                Path path = Paths.get(directory).toAbsolutePath().normalize();
                if (!Files.exists(path)) {
                    Files.createDirectories(path);
                    log.info("Created directory: {}", path);
                } else {
                    log.info("Directory already exists: {}", path);
                }
            }
        } catch (Exception e) {
            log.error("Failed to create directory: {}", directory, e);
            throw new RuntimeException("Failed to create directory: " + directory, e);
        }
    }
}
