package com.example.Spotify.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SongStreamMetadataDTO {
    private Long songId;
    private String name;
    private String cover; // base64 encoded cover image
    private Double totalDurationSeconds;
    private Long totalChunks;
    private List<ChunkMetadata> chunks;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ChunkMetadata {
        private Integer chunkIndex;
        private Double durationSeconds;
        private Double startTimeSeconds;
        private Double endTimeSeconds;
    }
}
