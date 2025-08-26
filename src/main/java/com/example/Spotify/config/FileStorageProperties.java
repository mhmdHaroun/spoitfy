package com.example.Spotify.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "file.upload")
public class FileStorageProperties {
    private String baseDir;
    private String songsDir;
    private String coversDir;
    private String albumsDir;
    private List<String> allowedAudioTypes;
    private List<String> allowedImageTypes;
    private boolean enableValidation;
    private String maxAudioSize;
    private String maxImageSize;

    public long getMaxAudioSizeInBytes() {
        return parseSize(maxAudioSize);
    }

    public long getMaxImageSizeInBytes() {
        return parseSize(maxImageSize);
    }

    private long parseSize(String size) {
        if (size == null || size.isEmpty()) {
            return 0;
        }

        size = size.toUpperCase().trim();
        long multiplier = 1;

        if (size.endsWith("KB")) {
            multiplier = 1024;
            size = size.substring(0, size.length() - 2);
        } else if (size.endsWith("MB")) {
            multiplier = 1024 * 1024;
            size = size.substring(0, size.length() - 2);
        } else if (size.endsWith("GB")) {
            multiplier = 1024 * 1024 * 1024;
            size = size.substring(0, size.length() - 2);
        }

        try {
            return Long.parseLong(size.trim()) * multiplier;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
