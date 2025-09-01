package com.example.Spotify.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "song_info")
public class SongInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "song_info_id")
    private long id;
    private String title;
    private int likes;
    private int dislikes;
    private int playCount;
    private String songURL;
    private String songCoverURL;
    private Date publishDate;
    private boolean isPremium;
    private int popularity;
    private String genre;
    private Double durationSeconds; // Total duration of the song

    @OneToMany(mappedBy = "songInfo", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Comment> comments = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "artist_id")
    private Artist artist;

    @ManyToOne
    @JoinColumn(name = "album_id")
    private Album album;

    @OneToMany(mappedBy = "songInfo", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<LikedDislikedSong> likedDislikedSongs = new ArrayList<>();

    @OneToMany(mappedBy = "songInfo", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<SongPlaylistRelation> songPlaylistRelations = new ArrayList<>();

    @OneToMany(mappedBy = "songInfo", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<UserSongContinue> userSongContinues = new ArrayList<>();

    @OneToMany(mappedBy = "songInfo", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<SongChunk> songChunks = new ArrayList<>();
}
