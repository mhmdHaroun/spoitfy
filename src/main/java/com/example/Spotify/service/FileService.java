package com.example.Spotify.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {
    @Deprecated
    String storeFile(MultipartFile multipartFile, String location);

    @Deprecated
    Resource loadFileAsResource(String location);
}
