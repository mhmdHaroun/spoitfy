package com.example.Spotify.util;

import com.example.Spotify.config.FileStorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class FileValidationUtil {

    private final FileStorageProperties fileStorageProperties;

    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult error(String message) {
            return new ValidationResult(false, message);
        }

        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }
    }

    public ValidationResult validateAudioFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ValidationResult.error("Audio file is required");
        }

        // Check file size
        if (file.getSize() > fileStorageProperties.getMaxAudioSizeInBytes()) {
            return ValidationResult.error("Audio file size exceeds maximum allowed size of " +
                fileStorageProperties.getMaxAudioSize());
        }

        // Check content type
        String contentType = file.getContentType();
        if (contentType == null || !fileStorageProperties.getAllowedAudioTypes().contains(contentType.toLowerCase())) {
            return ValidationResult.error("Invalid audio file type. Allowed types: " +
                String.join(", ", fileStorageProperties.getAllowedAudioTypes()));
        }

        // Check file extension
        String filename = file.getOriginalFilename();
        if (filename == null || !hasValidAudioExtension(filename)) {
            return ValidationResult.error("Invalid audio file extension");
        }

        return ValidationResult.success();
    }

    public ValidationResult validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ValidationResult.error("Image file is required");
        }

        // Check file size
        if (file.getSize() > fileStorageProperties.getMaxImageSizeInBytes()) {
            return ValidationResult.error("Image file size exceeds maximum allowed size of " +
                fileStorageProperties.getMaxImageSize());
        }

        // Check content type
        String contentType = file.getContentType();
        if (contentType == null || !fileStorageProperties.getAllowedImageTypes().contains(contentType.toLowerCase())) {
            return ValidationResult.error("Invalid image file type. Allowed types: " +
                String.join(", ", fileStorageProperties.getAllowedImageTypes()));
        }

        // Check file extension
        String filename = file.getOriginalFilename();
        if (filename == null || !hasValidImageExtension(filename)) {
            return ValidationResult.error("Invalid image file extension");
        }

        return ValidationResult.success();
    }

    private boolean hasValidAudioExtension(String filename) {
        List<String> validExtensions = Arrays.asList(".mp3", ".wav", ".flac", ".m4a");
        return validExtensions.stream()
            .anyMatch(ext -> filename.toLowerCase().endsWith(ext));
    }

    private boolean hasValidImageExtension(String filename) {
        List<String> validExtensions = Arrays.asList(".jpg", ".jpeg", ".png", ".webp");
        return validExtensions.stream()
            .anyMatch(ext -> filename.toLowerCase().endsWith(ext));
    }

    public String sanitizeFilename(String filename) {
        if (filename == null) {
            return "unknown";
        }

        // Remove path separators and other dangerous characters
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_")
                      .replaceAll("_{2,}", "_")
                      .toLowerCase();
    }
}
