package com.example.Spotify.repository;

import com.example.Spotify.model.SongChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SongChunkRepository extends JpaRepository<SongChunk, Long> {

    List<SongChunk> findBySongInfoIdOrderByChunkIndex(Long songInfoId);

    Optional<SongChunk> findBySongInfoIdAndChunkIndex(Long songInfoId, Integer chunkIndex);

    @Query("SELECT COUNT(c) FROM SongChunk c WHERE c.songInfo.id = :songInfoId")
    Long countBySongInfoId(@Param("songInfoId") Long songInfoId);

    void deleteBySongInfoId(Long songInfoId);
}
