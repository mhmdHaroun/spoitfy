package com.example.Spotify.model.elasticsearch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "songs-lyrics")
@Setting(settingPath = "elasticsearch/song-lyrics-settings.json")
public class SongLyricsDocument {

    @Id
    private String id; // This will be songId from PostgreSQL

    @Field(type = FieldType.Text, analyzer = "standard")
    private String title;

    @Field(type = FieldType.Text, analyzer = "lyrics_analyzer")
    private String lyrics;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String artistName;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String albumName;

    @Field(type = FieldType.Keyword)
    private Long songId; // Reference to PostgreSQL song ID

    @Field(type = FieldType.Keyword)
    private Long artistId;

    @Field(type = FieldType.Keyword)
    private Long albumId;

    @Field(type = FieldType.Integer)
    private Integer playCount;

    @Field(type = FieldType.Integer)
    private Integer likes;

    @Field(type = FieldType.Integer)
    private Integer dislikes;

    @Field(type = FieldType.Boolean)
    private Boolean isPremium;

    @Field(type = FieldType.Date)
    private LocalDateTime publishDate;

    @Field(type = FieldType.Date)
    private LocalDateTime indexedAt;

    // Additional fields for enhanced search
    @Field(type = FieldType.Text, analyzer = "keyword_lowercase")
    private String genre;

    @Field(type = FieldType.Integer)
    private Integer duration; // Duration in seconds

    // Computed field for search ranking
    @Field(type = FieldType.Double)
    private Double popularityScore;
}
