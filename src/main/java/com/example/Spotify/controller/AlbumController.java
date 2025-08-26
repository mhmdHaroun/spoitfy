package com.example.Spotify.controller;

import com.example.Spotify.dto.AlbumDTO;
import com.example.Spotify.dto.SongAlbumDTO;
import com.example.Spotify.dto.UpdateAlbumRequest;
import com.example.Spotify.model.Album;
import com.example.Spotify.service.AlbumService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@Slf4j
@RestController
@RequestMapping("/api/v1/albums")
@RequiredArgsConstructor
public class AlbumController {
    private final AlbumService albumService;

    @PostMapping
    @PreAuthorize("hasRole('ARTIST')")
    public ResponseEntity<?> createAlbum(
            @RequestParam("name") String name,
            @RequestParam("artistId") Long artistId,
            @RequestParam(value = "isPremium", defaultValue = "false") Boolean isPremium,
            @RequestParam("albumCover") MultipartFile albumCover
    ) {
        log.info("Album creation request received for name: {} by artist ID: {}", name, artistId);

        // Validate input parameters
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body("Album name is required");
        }

        if (artistId == null) {
            return ResponseEntity.badRequest()
                    .body("Artist ID is required");
        }

        if (albumCover == null || albumCover.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body("Album cover is required");
        }

        try {
            AlbumService.AlbumUploadResult result = albumService.createAlbum(AlbumDTO.builder()
                    .name(name.trim())
                    .artistId(artistId)
                    .isPremium(isPremium)
                    .albumCover(albumCover)
                    .build());

            if (result.isSuccess()) {
                log.info("Album created successfully: {} with ID: {}", name, result.getAlbum().getId());
                return ResponseEntity.ok(result.getAlbum());
            } else {
                log.warn("Album creation failed for name {}: {}", name, result.getErrorMessage());
                return ResponseEntity.badRequest()
                        .body("Album creation failed: " + result.getErrorMessage());
            }
        } catch (Exception e) {
            log.error("Unexpected error during album creation for name: {}", name, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred during album creation");
        }
    }

    @PostMapping("/legacy")
    @PreAuthorize("hasRole('ARTIST')")
    @Deprecated
    public ResponseEntity<String> addAlbumLegacy(
            @RequestParam("name") String name,
            @RequestParam("artistId") Long artistId,
            @RequestParam("isPremium") Boolean isPremium,
            @RequestParam("albumCover") MultipartFile albumCover
    ) {
        log.warn("Using deprecated legacy album creation endpoint");
        return albumService.addAlbum(AlbumDTO.builder()
                .name(name)
                .artistId(artistId)
                .isPremium(isPremium)
                .albumCover(albumCover)
                .build());
    }

    @GetMapping
    public ResponseEntity<Page<Album>> getAllAlbums(
            Pageable pageable
    ) {
        return ResponseEntity.ok(albumService.getAlbumsSortedByPopularity(pageable));
    }

    @GetMapping("/{albumId}")
    public ResponseEntity<Album> getAlbumById(
            @PathVariable Long albumId
    ) {
        return ResponseEntity.ok(albumService.getAlbum(albumId));
    }

    @DeleteMapping
    public ResponseEntity<String> deleteAlbum(
            @RequestParam("artistId") Long artistId,
            @RequestParam("name") String name) {
        return ResponseEntity.ok(albumService.deleteAlbum(artistId, name));
    }

    @PutMapping
    public ResponseEntity<Album> updateAlbum(
            @RequestParam Long userId,
            @RequestParam Long albumId,
            @RequestParam String albumName,
            @RequestParam Boolean isPremium,
            @RequestParam MultipartFile albumCover
    ){
        return ResponseEntity.ok(albumService.updateAlbum(UpdateAlbumRequest.builder()
                .userId(userId)
                .albumId(albumId)
                .albumName(albumName)
                .isPremium(isPremium)
                .albumCover(albumCover)
                .build())
        );
    }

    @PutMapping("/addSong")
    public ResponseEntity<Album> addSong(
            @RequestBody SongAlbumDTO songAlbumDTO
    ){
        return ResponseEntity.ok(albumService.addSong(songAlbumDTO));
    }

    @PutMapping("/removeSong")
    public ResponseEntity<Album> removeSong(
            @RequestBody SongAlbumDTO songAlbumDTO
    ){
        return ResponseEntity.ok(albumService.removeSong(songAlbumDTO));
    }
}
