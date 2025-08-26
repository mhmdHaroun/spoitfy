package com.example.Spotify.controller;

import com.example.Spotify.dto.SearchResultDTO;
import com.example.Spotify.dto.SongPlayDTO;
import com.example.Spotify.model.SongInfo;
import com.example.Spotify.service.SongService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/songs")
public class SongController {

    private final SongService songService;

    @PostMapping
    @PreAuthorize("hasRole('ARTIST')")
    public ResponseEntity<?> uploadSongAndCover(
            @RequestParam String title,
            @RequestParam MultipartFile songFile,
            @RequestParam MultipartFile coverImageFile
    ){
        log.info("Song upload request received for title: {}", title);

        // Validate input parameters
        if (title == null || title.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body("Song title is required");
        }

        if (songFile == null || songFile.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body("Song file is required");
        }

        if (coverImageFile == null || coverImageFile.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body("Cover image file is required");
        }

        try {
            SongService.SongUploadResult result = songService.uploadSongWithCover(songFile, coverImageFile, title.trim());

            if (result.isSuccess()) {
                log.info("Song uploaded successfully: {}", title);
                return ResponseEntity.ok(result.getSongInfo());
            } else {
                log.warn("Song upload failed for title {}: {}", title, result.getErrorMessage());
                return ResponseEntity.badRequest()
                        .body("Upload failed: " + result.getErrorMessage());
            }
        } catch (Exception e) {
            log.error("Unexpected error during song upload for title: {}", title, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred during upload");
        }
    }

    @PostMapping("/legacy")
    @PreAuthorize("hasRole('ARTIST')")
    @Deprecated
    public ResponseEntity<SongInfo> uploadSongAndCoverLegacy(
            @RequestParam String title,
            @RequestParam MultipartFile songFile,
            @RequestParam MultipartFile coverImageFile
    ){
        System.out.println("post song is called");
        log.warn("Using deprecated legacy upload endpoint");
        songService.addSongAndCover(songFile, coverImageFile, title);
        SongInfo songInfo = songService.addSongInfo(title);
        return ResponseEntity.ok(songInfo);
    }

    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SearchResultDTO> search(
            @RequestParam String title
    ){
        log.info("Search request received for title: {}", title);
        return ResponseEntity.ok(songService.findSongByTitle(title));
    }

    @GetMapping("/stream")
    public ResponseEntity<SongPlayDTO> downloadFile(
            @RequestParam Long songId
    ) {
        return ResponseEntity.ok(songService.streamSong(songId));
    }

    @PutMapping("/like/{songId}/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SongInfo> like(
            @PathVariable Long songId,
            @PathVariable Long userId
    ){
        return ResponseEntity.ok(songService.like(songId, userId));
    }

    @PutMapping("/dislike/{songId}/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SongInfo> dislike(
            @PathVariable Long songId,
            @PathVariable Long userId
    ){
        return ResponseEntity.ok(songService.dislike(songId, userId));
    }

    @GetMapping("/allLiked/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SongInfo>> getAllLikedSongs(
            @PathVariable Long userId
    ){
        return ResponseEntity.ok(songService.getUserLikedSongs(userId));
    }

    @GetMapping("/allDisliked/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SongInfo>> getAllDislikedSongs(
            @PathVariable Long userId
    ){
        return ResponseEntity.ok(songService.getUserDislikedSongs(userId));
    }

    @DeleteMapping("/{songId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ARTIST')")
    public ResponseEntity<String> deleteSong(
            @PathVariable Long songId
    ) {
        return ResponseEntity.ok(songService.deleteSong(songId));
    }
}
