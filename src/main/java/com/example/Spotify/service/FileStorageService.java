package com.example.Spotify.service;

import com.example.Spotify.util.FileValidationUtil;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    class FileUploadResult {
        private final boolean success;
        private final String filePath;
        private final String fileName;
        private final String errorMessage;

        private FileUploadResult(boolean success, String filePath, String fileName, String errorMessage) {
            this.success = success;
            this.filePath = filePath;
            this.fileName = fileName;
            this.errorMessage = errorMessage;
        }

        public static FileUploadResult success(String filePath, String fileName) {
            return new FileUploadResult(true, filePath, fileName, null);
        }

        public static FileUploadResult error(String errorMessage) {
            return new FileUploadResult(false, null, null, errorMessage);
        }

        public boolean isSuccess() { return success; }
        public String getFilePath() { return filePath; }
        public String getFileName() { return fileName; }
        public String getErrorMessage() { return errorMessage; }
    }

    FileUploadResult storeAudioFile(MultipartFile file, String customName);
    FileUploadResult storeImageFile(MultipartFile file, String customName);
    FileUploadResult storeAlbumCover(MultipartFile file, String customName);

    Resource loadFileAsResource(String filePath);
    boolean deleteFile(String filePath);
    boolean fileExists(String filePath);

    String generateUniqueFileName(String originalName, String extension);
}
