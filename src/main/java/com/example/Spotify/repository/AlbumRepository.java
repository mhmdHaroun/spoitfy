package com.example.Spotify.repository;

import com.example.Spotify.model.Album;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AlbumRepository extends JpaRepository<Album, Long> {
    @Query( "SELECT a " +
            "From Album a " +
            "LEFT JOIN a.songInfo s " +
            "GROUP BY a " +
            "ORDER BY " +
            "SUM (2 * s.likes + s.playCount - s.dislikes) DESC")
    Page<Album> getAlbumsSortedByPopularity(Pageable pageable);

    @Query("SELECT a FROM Album a WHERE a.name = :name AND a.artist.id = :artistId")
    Optional<Album> findByNameAndArtistId(@Param("name") String name, @Param("artistId") Long artistId);
}
