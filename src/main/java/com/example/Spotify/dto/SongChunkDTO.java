package com.example.Spotify.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SongChunkDTO {
    private Integer chunkIndex;
    private Double durationSeconds;
    private Double startTimeSeconds;
    private Double endTimeSeconds;
    private String chunkData; // base64 encoded chunk data
}
