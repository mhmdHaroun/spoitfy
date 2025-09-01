package com.example.Spotify.service.impl;

import com.example.Spotify.dto.SearchResultDTO;
import com.example.Spotify.dto.SongChunkDTO;
import com.example.Spotify.dto.SongPlayDTO;
import com.example.Spotify.dto.SongSearchDTO;
import com.example.Spotify.dto.SongStreamMetadataDTO;
import com.example.Spotify.exceptions.ResourceNotFoundException;
import com.example.Spotify.model.*;
import com.example.Spotify.repository.*;
import com.example.Spotify.service.AudioChunkingService;
import com.example.Spotify.service.FileService;
import com.example.Spotify.service.FileStorageService;
import com.example.Spotify.service.SongService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


@Slf4j
@Service
@RequiredArgsConstructor
public class SongServiceImpl implements SongService {

    @Deprecated
    private static final String songsLocation = "src/main/java/com/example/Spotify/model/songs/";
    @Deprecated
    private static final String songsCoverLocation = "src/main/java/com/example/Spotify/model/songs_cover/";

    private final FileService fileService;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;
    private final SongInfoRepository songInfoRepository;
    private final LikeDislikeSongRepository likeDislikeSongRepository;
    private final AudioChunkingService audioChunkingService;

    @Override
    @Transactional
    public SongUploadResult uploadSongWithCover(MultipartFile songFile, MultipartFile coverImageFile, String title) {
        try {
            log.info("Starting upload process for song: {}", title);

            // Upload cover image first
            FileStorageService.FileUploadResult coverResult = fileStorageService.storeImageFile(coverImageFile, title);
            if (!coverResult.isSuccess()) {
                log.error("Failed to upload cover image: {}", coverResult.getErrorMessage());
                return SongUploadResult.error("Cover upload failed: " + coverResult.getErrorMessage());
            }

            // Create song info in database first (without audio URL)
            SongInfo songInfo = SongInfo.builder()
                    .title(title)
                    .songCoverURL(coverResult.getFilePath())
                    .likes(0)
                    .dislikes(0)
                    .playCount(0)
                    .publishDate(new Date())
                    .isPremium(false)
                    .likedDislikedSongs(new ArrayList<>())
                    .songPlaylistRelations(new ArrayList<>())
                    .songChunks(new ArrayList<>())
                    .build();

            songInfo = songInfoRepository.save(songInfo);

            // Chunk the audio file
            AudioChunkingService.ChunkingResult chunkingResult =
                audioChunkingService.chunkAudioFile(songFile, songInfo, 10.0);

            if (!chunkingResult.isSuccess()) {
                log.error("Failed to chunk audio file: {}", chunkingResult.getErrorMessage());
                // Clean up cover file and song info
                fileStorageService.deleteFile(coverResult.getFilePath());
                songInfoRepository.deleteById(songInfo.getId());
                return SongUploadResult.error("Audio chunking failed: " + chunkingResult.getErrorMessage());
            }

            // Update song info with duration
            songInfo.setDurationSeconds(chunkingResult.getTotalDurationSeconds());
            songInfo = songInfoRepository.save(songInfo);

            log.info("Song uploaded and chunked successfully: {} with ID: {}, {} chunks created",
                    title, songInfo.getId(), chunkingResult.getChunks().size());
            return SongUploadResult.success(songInfo);

        } catch (Exception e) {
            log.error("Unexpected error during song upload: {}", title, e);
            return SongUploadResult.error("Upload failed due to unexpected error: " + e.getMessage());
        }
    }

    @Override
    public SongStreamMetadataDTO getSongStreamMetadata(Long songId) {
        try {
            Optional<SongInfo> songInfoOpt = songInfoRepository.findById(songId);
            if (songInfoOpt.isEmpty()) {
                throw new ResourceNotFoundException("Song not found with ID: " + songId);
            }

            SongInfo songInfo = songInfoOpt.get();
            List<SongChunk> chunks = audioChunkingService.getChunksMetadata(songId);

            // Load cover file
            Resource coverFile = fileStorageService.loadFileAsResource(songInfo.getSongCoverURL());
            String coverBase64 = encodeFileToBase64(coverFile);

            // Build chunk metadata
            List<SongStreamMetadataDTO.ChunkMetadata> chunkMetadataList = chunks.stream()
                .map(chunk -> SongStreamMetadataDTO.ChunkMetadata.builder()
                    .chunkIndex(chunk.getChunkIndex())
                    .durationSeconds(chunk.getDurationSeconds())
                    .startTimeSeconds(chunk.getStartTimeSeconds())
                    .endTimeSeconds(chunk.getEndTimeSeconds())
                    .build())
                .toList();

            return SongStreamMetadataDTO.builder()
                    .songId(songInfo.getId())
                    .name(songInfo.getTitle())
                    .cover(coverBase64)
                    .totalDurationSeconds(songInfo.getDurationSeconds())
                    .totalChunks((long) chunks.size())
                    .chunks(chunkMetadataList)
                    .build();

        } catch (Exception e) {
            log.error("Error getting stream metadata for song ID: {}", songId, e);
            throw new RuntimeException("Failed to get stream metadata: " + e.getMessage());
        }
    }

    @Override
    public SongChunkDTO getChunk(Long songId, Integer chunkIndex) {
        try {
            Optional<SongInfo> songInfoOpt = songInfoRepository.findById(songId);
            if (songInfoOpt.isEmpty()) {
                throw new ResourceNotFoundException("Song not found with ID: " + songId);
            }

            List<SongChunk> chunks = audioChunkingService.getChunksMetadata(songId);
            SongChunk requestedChunk = chunks.stream()
                .filter(chunk -> chunk.getChunkIndex().equals(chunkIndex))
                .findFirst()
                .orElse(null);

            if (requestedChunk == null) {
                throw new ResourceNotFoundException("Chunk not found: songId=" + songId + ", chunkIndex=" + chunkIndex);
            }

            String chunkData = audioChunkingService.getChunkAsBase64(songId, chunkIndex);
            if (chunkData == null) {
                throw new RuntimeException("Failed to load chunk data");
            }

            // Increment play count only for the first chunk (index 0)
            if (chunkIndex == 0) {
                SongInfo songInfo = songInfoOpt.get();
                songInfo.setPlayCount(songInfo.getPlayCount() + 1);
                songInfoRepository.save(songInfo);
            }

            return SongChunkDTO.builder()
                    .chunkIndex(requestedChunk.getChunkIndex())
                    .durationSeconds(requestedChunk.getDurationSeconds())
                    .startTimeSeconds(requestedChunk.getStartTimeSeconds())
                    .endTimeSeconds(requestedChunk.getEndTimeSeconds())
                    .chunkData(chunkData)
                    .build();

        } catch (Exception e) {
            log.error("Error getting chunk: songId={}, chunkIndex={}", songId, chunkIndex, e);
            throw new RuntimeException("Failed to get chunk: " + e.getMessage());
        }
    }

    @Override
    @Deprecated
    public void addSongAndCover(MultipartFile songFile, MultipartFile coverImageFile, String name) {
        log.warn("Using deprecated addSongAndCover method");
        System.out.println(fileService.storeFile(songFile, songsLocation + name + ".mp3"));
        System.out.println(fileService.storeFile(coverImageFile, songsCoverLocation + name + ".jpg"));
    }

    @Override
    @Deprecated
    public SongInfo addSongInfo(String title) {
        log.warn("Using deprecated addSongInfo method");
        return songInfoRepository.save(
            SongInfo.builder()
                    .title(title)
                    .songCoverURL(songsCoverLocation + title + ".jpg")
                    .songURL(songsLocation + title + ".mp3")
                    .likes(0)
                    .dislikes(0)
                    .playCount(0)
                    .publishDate(new Date())
                    .isPremium(false)
                    .likedDislikedSongs(new ArrayList<>())
                    .songPlaylistRelations(new ArrayList<>())
                    .build()
        );
    }

    @Override
    public SongPlayDTO streamSong(Long songId) {
        try {
            Optional<SongInfo> songInfoOpt = songInfoRepository.findById(songId);
            if (songInfoOpt.isEmpty()) {
                throw new ResourceNotFoundException("Song not found with ID: " + songId);
            }

            SongInfo songInfo = songInfoOpt.get();

            // Load audio file
            Resource songFile = fileStorageService.loadFileAsResource(songInfo.getSongURL());
            String songBase64 = encodeFileToBase64(songFile);

            // Load cover file
            Resource coverFile = fileStorageService.loadFileAsResource(songInfo.getSongCoverURL());
            String coverBase64 = encodeFileToBase64(coverFile);

            // Increment play count
            songInfo.setPlayCount(songInfo.getPlayCount() + 1);
            songInfoRepository.save(songInfo);

            return SongPlayDTO.builder()
                    .name(songInfo.getTitle())
                    .song(songBase64)
                    .cover(coverBase64)
                    .build();

        } catch (Exception e) {
            log.error("Error streaming song with ID: {}", songId, e);
            throw new RuntimeException("Failed to stream song: " + e.getMessage());
        }
    }

    @Override
    public String getSongNameById(Long songId) {
        Optional<SongInfo> songInfoOpt = songInfoRepository.findById(songId);
        if (songInfoOpt.isEmpty()) {
            throw new ResourceNotFoundException("Song not found with ID: " + songId);
        }
        return songInfoOpt.get().getTitle();
    }


    @Override
    public Resource loadFileAsResource(String fileName) {
        Path fileStorageLocation = Paths.get(songsCoverLocation).toAbsolutePath().normalize();
        if (fileName.contains("mp3")){
            fileStorageLocation = Paths.get(songsLocation).toAbsolutePath().normalize();
        }
        try {
            Path filePath = fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("File not found " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("File not found " + fileName, ex);
        }
    }

    @Override
    public SearchResultDTO findSongByTitle(String title) {
        List<SongSearchDTO> songSearchDtoList = songInfoRepository.findByTitle(title);
        if (songSearchDtoList.isEmpty()) {
            System.out.println("song info not found");
            throw new ResourceNotFoundException("song info not found");
        }

        songSearchDtoList.sort((s1, s2) -> s2.getPopularity() - s1.getPopularity());
        SongInfo mainSongInfo = songInfoRepository.findById(songSearchDtoList.get(0).getId()).orElse(null);
        assert mainSongInfo != null;
        Artist artist = mainSongInfo.getArtist();
        Album album = mainSongInfo.getAlbum();
        return SearchResultDTO.builder()
                .songSearchDtoList(songSearchDtoList)
                .artist(artist)
                .songAlbum(album)
                .build();
    }

    @Override
    public SongInfo like(long songId, long userId) {
        Optional<SongInfo> songInfoOpt = songInfoRepository.findById(songId);
        if(songInfoOpt.isEmpty()) {
            System.out.println("Song Not found");
            throw new ResourceNotFoundException("Song Not found");
        }
        SongInfo songInfo = songInfoOpt.get();
        songInfo.setLikes(songInfo.getLikes() + 1);
        Optional<User> userOpt = userRepository.findById(userId);
        if(userOpt.isEmpty()) {
            System.out.println("User Not found");
            throw new ResourceNotFoundException("User Not found");
        }
        User user = userOpt.get();
        Optional<LikedDislikedSong> likeDislikeSongOpt= likeDislikeSongRepository.findByUserAndSongInfo(user.getId(), songInfo.getId());
        LikedDislikedSong likedDisLikeSong = new LikedDislikedSong();
        if(likeDislikeSongOpt.isPresent()) {
            likedDisLikeSong = likeDislikeSongOpt.get();
        }

        likedDisLikeSong.setUser(user);
        likedDisLikeSong.setSongInfo(songInfo);
        likedDisLikeSong.setFlag(true);
        songInfoRepository.save(songInfo);
        likeDislikeSongRepository.save(likedDisLikeSong);
        return songInfo;
    }

    @Override
    public SongInfo dislike(long songId, long userId) {
        Optional<SongInfo> songInfoOpt = songInfoRepository.findById(songId);
        if(songInfoOpt.isEmpty()) {
            System.out.println("Song Not found");
            throw new ResourceNotFoundException("Song Not found");
        }
        SongInfo songInfo = songInfoOpt.get();
        songInfo.setDislikes(songInfo.getDislikes() + 1);
        Optional<User> userOpt = userRepository.findById(userId);
        if(userOpt.isEmpty()) {
            System.out.println("User Not found");
            throw new ResourceNotFoundException("User Not found");
        }
        User user = userOpt.get();
        Optional<LikedDislikedSong> likeDislikeSongOpt= likeDislikeSongRepository.findByUserAndSongInfo(user.getId(), songInfo.getId());
        LikedDislikedSong likedDisLikeSong = new LikedDislikedSong();
        if(likeDislikeSongOpt.isPresent()) {
            likedDisLikeSong = likeDislikeSongOpt.get();
        }
        likedDisLikeSong.setUser(user);
        likedDisLikeSong.setSongInfo(songInfo);
        likedDisLikeSong.setFlag(false);
        songInfoRepository.save(songInfo);
        likeDislikeSongRepository.save(likedDisLikeSong);
        return songInfo;
    }

    @Override
    public List<SongInfo> getUserLikedSongs(long userId) {
        List<SongInfo> likedSongs = likeDislikeSongRepository.findLikedSongsByUserId(userId);
        System.out.println(likedSongs);
        return likedSongs;
    }

    @Override
    public List<SongInfo> getUserDislikedSongs(long userId) {
        List<SongInfo> dislikedSongs = likeDislikeSongRepository.findDislikedSongsByUserId(userId);
        System.out.println(dislikedSongs);
        return dislikedSongs;
    }

    private String encodeFileToBase64(Resource resource) {
        try {
            byte[] fileContent = Files.readAllBytes(resource.getFile().toPath());
            return Base64.getEncoder().encodeToString(fileContent);
        } catch (IOException e) {
            throw new RuntimeException("Could not read file: " + resource.getFilename(), e);
        }
    }

    @Override
    @Transactional
    public String deleteSong(Long songId) {
        try {
            Optional<SongInfo> songInfoOpt = songInfoRepository.findById(songId);
            if (songInfoOpt.isEmpty()) {
                log.warn("Attempted to delete non-existent song with ID: {}", songId);
                return "Song not found";
            }

            SongInfo songInfo = songInfoOpt.get();
            String songTitle = songInfo.getTitle();

            // Delete chunks first
            audioChunkingService.deleteChunks(songId);

            // Delete associated files
            boolean audioDeleted = true;
            boolean coverDeleted = true;

            if (songInfo.getSongURL() != null) {
                audioDeleted = fileStorageService.deleteFile(songInfo.getSongURL());
                if (!audioDeleted) {
                    log.warn("Failed to delete audio file: {}", songInfo.getSongURL());
                }
            }

            if (songInfo.getSongCoverURL() != null) {
                coverDeleted = fileStorageService.deleteFile(songInfo.getSongCoverURL());
                if (!coverDeleted) {
                    log.warn("Failed to delete cover file: {}", songInfo.getSongCoverURL());
                }
            }

            // Delete database record (this will cascade delete chunks due to orphanRemoval = true)
            songInfoRepository.deleteById(songId);

            String result = "Song '" + songTitle + "' and its chunks deleted successfully";
            if (!audioDeleted || !coverDeleted) {
                result += " (some files could not be deleted)";
            }

            log.info("Song and chunks deleted: {} (ID: {})", songTitle, songId);
            return result;

        } catch (Exception e) {
            log.error("Error deleting song with ID: {}", songId, e);
            return "Failed to delete song: " + e.getMessage();
        }
    }
}
