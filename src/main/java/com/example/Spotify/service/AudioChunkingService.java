package com.example.Spotify.service;

import com.example.Spotify.model.SongChunk;
import com.example.Spotify.model.SongInfo;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AudioChunkingService {

    class ChunkingResult {
        private final boolean success;
        private final List<SongChunk> chunks;
        private final String errorMessage;
        private final double totalDurationSeconds;

        private ChunkingResult(boolean success, List<SongChunk> chunks, String errorMessage, double totalDurationSeconds) {
            this.success = success;
            this.chunks = chunks;
            this.errorMessage = errorMessage;
            this.totalDurationSeconds = totalDurationSeconds;
        }

        public static ChunkingResult success(List<SongChunk> chunks, double totalDurationSeconds) {
            return new ChunkingResult(true, chunks, null, totalDurationSeconds);
        }

        public static ChunkingResult error(String errorMessage) {
            return new ChunkingResult(false, null, errorMessage, 0.0);
        }

        public boolean isSuccess() { return success; }
        public List<SongChunk> getChunks() { return chunks; }
        public String getErrorMessage() { return errorMessage; }
        public double getTotalDurationSeconds() { return totalDurationSeconds; }
    }

    /**
     * Splits an audio file into chunks of maximum 10 seconds each
     * @param audioFile The audio file to chunk
     * @param songInfo The song info entity
     * @param maxChunkDurationSeconds Maximum duration for each chunk (default 10 seconds)
     * @return ChunkingResult containing the created chunks or error information
     */
    ChunkingResult chunkAudioFile(MultipartFile audioFile, SongInfo songInfo, double maxChunkDurationSeconds);

    /**
     * Retrieves a specific chunk for streaming
     * @param songId The song ID
     * @param chunkIndex The chunk index (0-based)
     * @return The chunk data as base64 string, or null if not found
     */
    String getChunkAsBase64(Long songId, Integer chunkIndex);

    /**
     * Gets the total number of chunks for a song
     * @param songId The song ID
     * @return Number of chunks
     */
    Long getChunkCount(Long songId);

    /**
     * Gets all chunks metadata for a song
     * @param songId The song ID
     * @return List of chunk metadata
     */
    List<SongChunk> getChunksMetadata(Long songId);

    /**
     * Deletes all chunks for a song
     * @param songId The song ID
     */
    void deleteChunks(Long songId);
}
