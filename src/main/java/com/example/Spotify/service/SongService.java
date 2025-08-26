package com.example.Spotify.service;

import com.example.Spotify.dto.SearchResultDTO;
import com.example.Spotify.dto.SongPlayDTO;
import com.example.Spotify.model.SongInfo;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface SongService {

    class SongUploadResult {
        private final boolean success;
        private final SongInfo songInfo;
        private final String errorMessage;

        private SongUploadResult(boolean success, SongInfo songInfo, String errorMessage) {
            this.success = success;
            this.songInfo = songInfo;
            this.errorMessage = errorMessage;
        }

        public static SongUploadResult success(SongInfo songInfo) {
            return new SongUploadResult(true, songInfo, null);
        }

        public static SongUploadResult error(String errorMessage) {
            return new SongUploadResult(false, null, errorMessage);
        }

        public boolean isSuccess() { return success; }
        public SongInfo getSongInfo() { return songInfo; }
        public String getErrorMessage() { return errorMessage; }
    }

    SongUploadResult uploadSongWithCover(MultipartFile songFile, MultipartFile coverImageFile, String title);

    @Deprecated
    void addSongAndCover(MultipartFile songFile, MultipartFile coverImageFile, String name);
    @Deprecated
    SongInfo addSongInfo(String title);

    SongPlayDTO streamSong(Long songId);
    String getSongNameById(Long songId);
    Resource loadFileAsResource(String title);
    SearchResultDTO findSongByTitle(String title);
    SongInfo like(long songId, long userId);
    SongInfo dislike(long songId, long userId);
    List<SongInfo> getUserLikedSongs(long userId);
    List<SongInfo> getUserDislikedSongs(long userId);
    String deleteSong(Long id);
}
