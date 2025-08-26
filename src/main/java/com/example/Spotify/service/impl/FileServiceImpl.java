package com.example.Spotify.service.impl;

import com.example.Spotify.service.FileService;
import com.example.Spotify.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileOutputStream;
import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final FileStorageService fileStorageService;

    @Override
    @Deprecated
    public String storeFile(MultipartFile multipartFile, String location) {
        try {
            byte[] fileBytes = multipartFile.getBytes();
            try (FileOutputStream fileWriter = new FileOutputStream(location)) {
                fileWriter.write(fileBytes);
                log.info("File stored successfully at: {}", location);
                return "Storing Success";
            }
        } catch (IOException e) {
            log.error("Failed to store file at location: {}", location, e);
            return "Storing Failed: " + e.getMessage();
        }
    }

    @Override
    @Deprecated
    public Resource loadFileAsResource(String location) {
        return fileStorageService.loadFileAsResource(location);
    }
}
