package com.example.Spotify.service;

import com.example.Spotify.dto.AlbumDTO;
import com.example.Spotify.dto.SongAlbumDTO;
import com.example.Spotify.dto.UpdateAlbumRequest;
import com.example.Spotify.model.Album;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

public interface AlbumService {

    class AlbumUploadResult {
        private final boolean success;
        private final Album album;
        private final String errorMessage;

        private AlbumUploadResult(boolean success, Album album, String errorMessage) {
            this.success = success;
            this.album = album;
            this.errorMessage = errorMessage;
        }

        public static AlbumUploadResult success(Album album) {
            return new AlbumUploadResult(true, album, null);
        }

        public static AlbumUploadResult error(String errorMessage) {
            return new AlbumUploadResult(false, null, errorMessage);
        }

        public boolean isSuccess() { return success; }
        public Album getAlbum() { return album; }
        public String getErrorMessage() { return errorMessage; }
    }

    AlbumUploadResult createAlbum(AlbumDTO albumDTO);

    @Deprecated
    ResponseEntity<String> addAlbum(AlbumDTO albumDTO);

    Page<Album> getAlbumsSortedByPopularity(Pageable pageable);
    Album getAlbum(Long albumId);
    String deleteAlbum(Long artistId, String title);
    Album updateAlbum(UpdateAlbumRequest updateAlbumRequest);
    Album addSong(SongAlbumDTO songAlbumDTO);
    Album removeSong(SongAlbumDTO songAlbumDTO);
}
