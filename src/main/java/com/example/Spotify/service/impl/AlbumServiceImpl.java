package com.example.Spotify.service.impl;

import com.example.Spotify.dto.AlbumDTO;
import com.example.Spotify.dto.SongAlbumDTO;
import com.example.Spotify.dto.UpdateAlbumRequest;
import com.example.Spotify.exceptions.ResourceNotFoundException;
import com.example.Spotify.model.Album;
import com.example.Spotify.model.Artist;
import com.example.Spotify.model.SongInfo;
import com.example.Spotify.repository.AlbumRepository;
import com.example.Spotify.repository.ArtistRepository;
import com.example.Spotify.repository.SongInfoRepository;
import com.example.Spotify.service.AlbumService;
import com.example.Spotify.service.FileService;
import com.example.Spotify.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Date;
import java.util.Optional;


@Slf4j
@Service
@RequiredArgsConstructor
public class AlbumServiceImpl implements AlbumService {

    @Deprecated
    private static final String albumCoverLocation = "src/main/java/com/example/Spotify/model/albums_cover/";

    private final AlbumRepository albumRepository;
    private final ArtistRepository artistRepository;
    private final ArtistServiceImpl artistServiceImpl;
    private final SongInfoRepository songInfoRepository;
    private final FileService fileService;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public AlbumUploadResult createAlbum(AlbumDTO albumDTO) {
        try {
            log.info("Creating album: {} for artist ID: {}", albumDTO.getName(), albumDTO.getArtistId());

            // Validate artist exists
            Optional<Artist> artistOpt = artistRepository.findById(albumDTO.getArtistId());
            if (artistOpt.isEmpty()) {
                return AlbumUploadResult.error("Artist not found with ID: " + albumDTO.getArtistId());
            }
            Artist artist = artistOpt.get();

            // Check if album with same name already exists for this artist
            Optional<Album> existingAlbum = albumRepository.findByNameAndArtistId(albumDTO.getName(), albumDTO.getArtistId());
            if (existingAlbum.isPresent()) {
                return AlbumUploadResult.error("Album with name '" + albumDTO.getName() + "' already exists for this artist");
            }

            // Upload album cover
            FileStorageService.FileUploadResult coverResult = fileStorageService.storeAlbumCover(
                albumDTO.getAlbumCover(), albumDTO.getName());

            if (!coverResult.isSuccess()) {
                log.error("Failed to upload album cover: {}", coverResult.getErrorMessage());
                return AlbumUploadResult.error("Cover upload failed: " + coverResult.getErrorMessage());
            }

            // Create album entity
            Album album = Album.builder()
                    .name(albumDTO.getName())
                    .isPremium(albumDTO.getIsPremium() != null ? albumDTO.getIsPremium() : false)
                    .artist(artist)
                    .coverURL(coverResult.getFilePath())
                    .releaseDate(new Date())
                    .build();

            album = albumRepository.save(album);

            log.info("Album created successfully: {} with ID: {}", albumDTO.getName(), album.getId());
            return AlbumUploadResult.success(album);

        } catch (Exception e) {
            log.error("Unexpected error creating album: {}", albumDTO.getName(), e);
            return AlbumUploadResult.error("Album creation failed due to unexpected error: " + e.getMessage());
        }
    }

    @Override
    @Deprecated
    public ResponseEntity<String> addAlbum(AlbumDTO albumDTO) {
        log.warn("Using deprecated addAlbum method");
        Optional<Artist> artistOpt = artistRepository.findById(albumDTO.getArtistId());
        if (artistOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Artist not found");
        }
        Artist artist = artistOpt.get();
        albumRepository.save(
                Album.builder()
                .name(albumDTO.getName())
                .isPremium(false)
                .artist(artist)
                .coverURL(albumCoverLocation + albumDTO.getName() + ".jpg")
                .releaseDate(new Date())
                .build());

        fileService.storeFile(albumDTO.getAlbumCover(), albumCoverLocation + albumDTO.getName() + ".jpg");
        return ResponseEntity.ok("Album cover is uploaded");
    }

    @Override
    public Page<Album> getAlbumsSortedByPopularity(Pageable pageable){
        return albumRepository.getAlbumsSortedByPopularity(pageable);
    }

    @Override
    public Album getAlbum(Long albumId){
        Optional<Album> albumOpt = albumRepository.findById(albumId);
        if (albumOpt.isEmpty()) {
            System.out.println("Album Not Found");
            throw new ResourceNotFoundException("Album Not Found");
        }
        return albumOpt.get();
    }

    @Override
    public String deleteAlbum(Long artistId, String title){
        Optional<Artist> artistOpt = artistRepository.findById(artistId);
        if (artistOpt.isEmpty()) {
            return "Artist not found";
        }
        Optional<Album> albumOpt = albumRepository.findById(artistId);
        if (albumOpt.isEmpty()){
            return "Album not found";
        }
        Album album = albumOpt.get();
        if (album.getArtist().getId() != artistId){
            return "Album does not belong to this Artist";
        }
        albumRepository.delete(album);
        return "deleted successfully";
    }

    @Override
    public Album updateAlbum(UpdateAlbumRequest updateAlbumRequest){
        Optional<Album> albumOpt = albumRepository.findById(updateAlbumRequest.getAlbumId());
        if (albumOpt.isEmpty()){
            System.out.println("Album not found");
            throw new ResourceNotFoundException("Album Not Found");
        }
        Album album = albumOpt.get();
        Optional<Artist> artistOpt = artistRepository.findById(album.getArtist().getId());
        if (artistOpt.isEmpty()){
            System.out.println("Artist not found");
            throw new ResourceNotFoundException("Artist Not Found");
        }
        Artist artist = artistOpt.get();
        if (album.getArtist().getId() != artist.getId()){
            System.out.println( "Album does not belong to this Artist");
            throw new RuntimeException("Album does not belong to this Artist");
        }
        String updatedCover = albumCoverLocation + album.getName() + ".jpg";
        fileService.storeFile(updateAlbumRequest.getAlbumCover(), updatedCover);
        return albumRepository.save(Album.builder()
                        .id(updateAlbumRequest.getAlbumId())
                        .name(updateAlbumRequest.getAlbumName())
                        .artist(artist)
                        .releaseDate(album.getReleaseDate())
                        .isPremium(album.isPremium())
                        .coverURL(updatedCover)
                        .build());

    }

    @Override
    public Album addSong(SongAlbumDTO songAlbumDTO){
        Optional<Artist> artistOpt = artistRepository.findById(songAlbumDTO.getArtistId());
        if (artistOpt.isEmpty()) {
            System.out.println("Artist not found");
            throw new ResourceNotFoundException("Artist Not Found");
        }
        Artist artist = artistOpt.get();
        Optional<Album> albumOpt = albumRepository.findById(songAlbumDTO.getAlbumId());
        if (albumOpt.isEmpty()){
            System.out.println("Album not found");
            throw new ResourceNotFoundException("Album Not Found");
        }
        Album album = albumOpt.get();
        if (album.getArtist().getId() == artist.getId()){
            System.out.println("Album does not belong to this Artist");
            throw new RuntimeException("Album does not belong to this Artist");
        }
        Optional<SongInfo> songInfoOpt = songInfoRepository.findById(songAlbumDTO.getSongId());
        if (songInfoOpt.isEmpty()){
            System.out.println("Song info not found");
            throw new ResourceNotFoundException("Song Info Not Found");
        }
        SongInfo songInfo = songInfoOpt.get();
        album.getSongInfo().add(songInfo);
        System.out.println("Song is added Successfully");
        return albumRepository.save(album);
    }

    @Override
    public Album removeSong(SongAlbumDTO songAlbumDTO){
        Optional<Artist> artistOpt = artistRepository.findById(songAlbumDTO.getArtistId());
        if (artistOpt.isEmpty()) {
            System.out.println("Artist not found");
            throw new ResourceNotFoundException("Artist Not Found");
        }
        Artist artist = artistOpt.get();
        Optional<Album> albumOpt = albumRepository.findById(songAlbumDTO.getAlbumId());
        if (albumOpt.isEmpty()){
            System.out.println("Album not found");
            throw new ResourceNotFoundException("Album Not Found");
        }
        Album album = albumOpt.get();
        if (album.getArtist().getId() != artist.getId()){
            System.out.println("Album does not belong to this Artist");
            throw new RuntimeException("Album does not belong to this Artist");
        }
        Optional<SongInfo> songInfoOpt = songInfoRepository.findById(songAlbumDTO.getSongId());
        if (songInfoOpt.isEmpty()){
            System.out.println("song not found");
            throw new ResourceNotFoundException("Song not found");
        }
        SongInfo songInfo = songInfoOpt.get();
        album.getSongInfo().remove(songInfo);
        System.out.println("song removed Successfully");
        return albumRepository.save(album);
    }
}
