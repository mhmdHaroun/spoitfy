package com.example.Spotify.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "song_chunks")
public class SongChunk {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chunk_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "song_info_id", nullable = false)
    private SongInfo songInfo;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Column(name = "chunk_url", nullable = false)
    private String chunkUrl;

    @Column(name = "duration_seconds")
    private Double durationSeconds;

    @Column(name = "start_time_seconds")
    private Double startTimeSeconds;

    @Column(name = "end_time_seconds")
    private Double endTimeSeconds;
}
