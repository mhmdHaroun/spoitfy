//package com.example.Spotify.repository;
//
//import com.example.Spotify.model.SongLyrics;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
//
//import java.util.List;
//import java.util.Optional;
//
//@Repository
//public interface SongLyricsRepository extends JpaRepository<SongLyrics, Long> {
//
//    Optional<SongLyrics> findBySongInfoId(Long songId);
//
//    List<SongLyrics> findByLanguage(String language);
//
//    List<SongLyrics> findByIsVerified(Boolean isVerified);
//
//    @Query("SELECT sl FROM SongLyrics sl WHERE sl.lyrics LIKE %:keyword%")
//    List<SongLyrics> findByLyricsContaining(@Param("keyword") String keyword);
//
//    @Query("SELECT sl FROM SongLyrics sl WHERE sl.songInfo.artist.id = :artistId")
//    List<SongLyrics> findByArtistId(@Param("artistId") Long artistId);
//
//    @Query("SELECT sl FROM SongLyrics sl WHERE sl.songInfo.album.id = :albumId")
//    List<SongLyrics> findByAlbumId(@Param("albumId") Long albumId);
//
//    @Query("SELECT COUNT(sl) FROM SongLyrics sl WHERE sl.isVerified = true")
//    Long countVerifiedLyrics();
//}
