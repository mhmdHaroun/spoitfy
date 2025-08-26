package com.example.Spotify.service.impl;

import com.example.Spotify.config.FileStorageProperties;
import com.example.Spotify.service.FileStorageService;
import com.example.Spotify.util.FileValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    private final FileStorageProperties fileStorageProperties;
    private final FileValidationUtil fileValidationUtil;

    private Path songsLocation;
    private Path coversLocation;
    private Path albumsLocation;

    @PostConstruct
    public void init() {
        try {
            this.songsLocation = Paths.get(fileStorageProperties.getSongsDir())
                    .toAbsolutePath().normalize();
            this.coversLocation = Paths.get(fileStorageProperties.getCoversDir())
                    .toAbsolutePath().normalize();
            this.albumsLocation = Paths.get(fileStorageProperties.getAlbumsDir())
                    .toAbsolutePath().normalize();

            // Create directories if they don't exist
            Files.createDirectories(songsLocation);
            Files.createDirectories(coversLocation);
            Files.createDirectories(albumsLocation);

            log.info("File storage initialized:");
            log.info("Songs directory: {}", songsLocation);
            log.info("Covers directory: {}", coversLocation);
            log.info("Albums directory: {}", albumsLocation);

        } catch (Exception ex) {
            log.error("Could not create upload directories", ex);
            throw new RuntimeException("Could not create upload directories", ex);
        }
    }

    @Override
    public FileUploadResult storeAudioFile(MultipartFile file, String customName) {
        if (fileStorageProperties.isEnableValidation()) {
            FileValidationUtil.ValidationResult validation = fileValidationUtil.validateAudioFile(file);
            if (!validation.isValid()) {
                return FileUploadResult.error(validation.getErrorMessage());
            }
        }

        try {
            String fileName = generateFileName(customName, getFileExtension(file.getOriginalFilename()));
            Path targetLocation = songsLocation.resolve(fileName);

            // Check for duplicates
            if (Files.exists(targetLocation)) {
                fileName = generateUniqueFileName(customName, getFileExtension(file.getOriginalFilename()));
                targetLocation = songsLocation.resolve(fileName);
            }

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            log.info("Audio file stored successfully: {}", fileName);
            return FileUploadResult.success(targetLocation.toString(), fileName);

        } catch (IOException ex) {
            log.error("Could not store audio file: {}", customName, ex);
            return FileUploadResult.error("Could not store audio file: " + ex.getMessage());
        }
    }

    @Override
    public FileUploadResult storeImageFile(MultipartFile file, String customName) {
        if (fileStorageProperties.isEnableValidation()) {
            FileValidationUtil.ValidationResult validation = fileValidationUtil.validateImageFile(file);
            if (!validation.isValid()) {
                return FileUploadResult.error(validation.getErrorMessage());
            }
        }

        try {
            String fileName = generateFileName(customName, getFileExtension(file.getOriginalFilename()));
            Path targetLocation = coversLocation.resolve(fileName);

            // Check for duplicates
            if (Files.exists(targetLocation)) {
                fileName = generateUniqueFileName(customName, getFileExtension(file.getOriginalFilename()));
                targetLocation = coversLocation.resolve(fileName);
            }

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            log.info("Image file stored successfully: {}", fileName);
            return FileUploadResult.success(targetLocation.toString(), fileName);

        } catch (IOException ex) {
            log.error("Could not store image file: {}", customName, ex);
            return FileUploadResult.error("Could not store image file: " + ex.getMessage());
        }
    }

    @Override
    public FileUploadResult storeAlbumCover(MultipartFile file, String customName) {
        if (fileStorageProperties.isEnableValidation()) {
            FileValidationUtil.ValidationResult validation = fileValidationUtil.validateImageFile(file);
            if (!validation.isValid()) {
                return FileUploadResult.error(validation.getErrorMessage());
            }
        }

        try {
            String fileName = generateFileName(customName, getFileExtension(file.getOriginalFilename()));
            Path targetLocation = albumsLocation.resolve(fileName);

            // Check for duplicates
            if (Files.exists(targetLocation)) {
                fileName = generateUniqueFileName(customName, getFileExtension(file.getOriginalFilename()));
                targetLocation = albumsLocation.resolve(fileName);
            }

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            log.info("Album cover stored successfully: {}", fileName);
            return FileUploadResult.success(targetLocation.toString(), fileName);

        } catch (IOException ex) {
            log.error("Could not store album cover: {}", customName, ex);
            return FileUploadResult.error("Could not store album cover: " + ex.getMessage());
        }
    }

    @Override
    public Resource loadFileAsResource(String filePath) {
        try {
            Path file = Paths.get(filePath).normalize();
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                log.warn("File not found or not readable: {}", filePath);
                throw new RuntimeException("File not found: " + filePath);
            }
        } catch (MalformedURLException ex) {
            log.error("File not found: {}", filePath, ex);
            throw new RuntimeException("File not found: " + filePath, ex);
        }
    }

    @Override
    public boolean deleteFile(String filePath) {
        try {
            Path file = Paths.get(filePath);
            boolean deleted = Files.deleteIfExists(file);
            if (deleted) {
                log.info("File deleted successfully: {}", filePath);
            } else {
                log.warn("File not found for deletion: {}", filePath);
            }
            return deleted;
        } catch (IOException ex) {
            log.error("Could not delete file: {}", filePath, ex);
            return false;
        }
    }

    @Override
    public boolean fileExists(String filePath) {
        try {
            Path file = Paths.get(filePath);
            return Files.exists(file);
        } catch (Exception ex) {
            log.error("Error checking file existence: {}", filePath, ex);
            return false;
        }
    }

    @Override
    public String generateUniqueFileName(String originalName, String extension) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String sanitizedName = fileValidationUtil.sanitizeFilename(originalName);

        return String.format("%s_%s_%s%s", sanitizedName, timestamp, uuid, extension);
    }

    private String generateFileName(String customName, String extension) {
        String sanitizedName = fileValidationUtil.sanitizeFilename(customName);
        return sanitizedName + extension;
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }

        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }

        return filename.substring(lastDotIndex).toLowerCase();
    }
}
