package com.example.Spotify.service.impl;

import com.example.Spotify.dto.SearchResultDTO;
import com.example.Spotify.dto.SongPlayDTO;
import com.example.Spotify.dto.SongSearchDTO;
import com.example.Spotify.exceptions.ResourceNotFoundException;
import com.example.Spotify.model.LikedDislikedSong;
import com.example.Spotify.model.SongInfo;
import com.example.Spotify.model.User;
import com.example.Spotify.repository.*;
import com.example.Spotify.service.FileService;
import com.example.Spotify.service.SongService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


@Service
@RequiredArgsConstructor
public class SongServiceImpl implements SongService {

    private static final String songsLocation = "src/main/java/com/example/Spotify/model/songs/";
    private static final String songsCoverLocation = "src/main/java/com/example/Spotify/model/songs_cover/";

    private final FileService fileService;
    private final UserRepository userRepository;
    private final SongInfoRepository songInfoRepository;
    private final LikeDislikeSongRepository likeDislikeSongRepository;


    @Override
    public void addSongAndCover(MultipartFile songFile, MultipartFile coverImageFile, String name) {
            System.out.println(fileService.storeFile(songFile, songsLocation + name + ".mp3"));
            System.out.println(fileService.storeFile(coverImageFile, songsCoverLocation + name + ".jpg"));
    }

    @Override
    public SongInfo addSongInfo(String title) {
        return songInfoRepository.save(
            SongInfo.builder()
                    .title(title)
                    .songCoverURL(songsLocation + title + ".jpg")
                    .songURL(songsCoverLocation + title + ".mp3")
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
        String songName = getSongNameById(songId);
        Resource songFile = loadFileAsResource(songName + ".mp3");
        String songBase64 = encodeFileToBase64(songFile);
        Resource coverFile = loadFileAsResource(songName + ".jpg");
        String coverBase64 = encodeFileToBase64(coverFile);
        return SongPlayDTO.builder()
                .name(songName)
                .song(songBase64)
                .cover(coverBase64)
                .build();
    }

    @Override
    public String getSongNameById(Long songId) {
        return songInfoRepository.findById(songId).get().getTitle();
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
        System.out.println("find Song By Title service is called");
        Optional<List<SongSearchDTO>> songSearchDtoListOpt = songInfoRepository.findByTitle(title);
        System.out.println(songSearchDtoListOpt);
        if (songSearchDtoListOpt.isEmpty()) {
            System.out.println("song info not found");
            throw new ResourceNotFoundException("song info not found");
        }
        List<SongSearchDTO> songInfoList = songSearchDtoListOpt.get();
        System.out.println(songInfoList);
        songInfoList.sort((s1, s2) -> s2.getPlayCount() - s1.getPlayCount());
        System.out.println(songInfoList);
        return SearchResultDTO.builder()
                .songSearchDtoList(songInfoList)
                //album
                //artist
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

}
