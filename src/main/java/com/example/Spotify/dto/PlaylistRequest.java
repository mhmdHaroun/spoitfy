package com.example.Spotify.dto;

import com.example.Spotify.enums.PlaylistStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PlaylistRequest {
    private Long userId;
    private String playlistName;
    private PlaylistStatus playlistStatus;
}
