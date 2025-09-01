package com.example.Spotify.service.impl;

import com.example.Spotify.model.SongChunk;
import com.example.Spotify.model.SongInfo;
import com.example.Spotify.repository.SongChunkRepository;
import com.example.Spotify.service.AudioChunkingService;
import com.example.Spotify.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AudioChunkingServiceImpl implements AudioChunkingService {

    private final SongChunkRepository songChunkRepository;
    private final FileStorageService fileStorageService;

    private static final double DEFAULT_MAX_CHUNK_DURATION = 10.0; // 10 seconds
    private static final String CHUNKS_DIRECTORY = "uploads/chunks/";
    private static final long CHUNK_SIZE_BYTES = 1024 * 1024; // 1MB chunks (approximate for 10 seconds of audio)

    @Override
    @Transactional
    public ChunkingResult chunkAudioFile(MultipartFile audioFile, SongInfo songInfo, double maxChunkDurationSeconds) {
        try {
            log.info("Starting audio chunking for song: {} with max chunk duration: {}s",
                    songInfo.getTitle(), maxChunkDurationSeconds);

            // Create chunks directory if it doesn't exist
            Path chunksDir = Paths.get(CHUNKS_DIRECTORY + songInfo.getId());
            Files.createDirectories(chunksDir);

            // Read the entire audio file into memory
            byte[] audioData = audioFile.getBytes();
            long totalSize = audioData.length;

            // Estimate total duration (rough approximation: 1MB = ~10 seconds for typical MP3)
            double estimatedDuration = (totalSize / (double) CHUNK_SIZE_BYTES) * maxChunkDurationSeconds;

            // Calculate number of chunks based on file size
            int numberOfChunks = (int) Math.ceil((double) totalSize / CHUNK_SIZE_BYTES);
            if (numberOfChunks == 0) numberOfChunks = 1;

            log.info("File size: {} bytes, estimated duration: {}s, will create {} chunks",
                    totalSize, estimatedDuration, numberOfChunks);

            List<SongChunk> chunks = new ArrayList<>();

            for (int i = 0; i < numberOfChunks; i++) {
                long startByte = (long) i * CHUNK_SIZE_BYTES;
                long endByte = Math.min(startByte + CHUNK_SIZE_BYTES, totalSize);
                int chunkSize = (int) (endByte - startByte);

                // Extract chunk data
                byte[] chunkData = new byte[chunkSize];
                System.arraycopy(audioData, (int) startByte, chunkData, 0, chunkSize);

                // Save chunk to file
                String chunkFileName = String.format("chunk_%03d.mp3", i);
                Path chunkPath = chunksDir.resolve(chunkFileName);
                Files.write(chunkPath, chunkData);

                // Calculate estimated timing for this chunk
                double chunkDuration = Math.min(maxChunkDurationSeconds,
                    estimatedDuration - (i * maxChunkDurationSeconds));
                double startTime = i * maxChunkDurationSeconds;
                double endTime = startTime + chunkDuration;

                // Create chunk entity
                SongChunk chunk = SongChunk.builder()
                        .songInfo(songInfo)
                        .chunkIndex(i)
                        .chunkUrl(chunkPath.toString())
                        .durationSeconds(chunkDuration)
                        .startTimeSeconds(startTime)
                        .endTimeSeconds(endTime)
                        .build();

                chunks.add(songChunkRepository.save(chunk));
                log.debug("Created chunk {}: {}s - {}s ({}s duration, {} bytes)",
                         i, startTime, endTime, chunkDuration, chunkSize);
            }

            log.info("Successfully created {} chunks for song: {}", chunks.size(), songInfo.getTitle());
            return ChunkingResult.success(chunks, estimatedDuration);

        } catch (Exception e) {
            log.error("Error chunking audio file for song: {}", songInfo.getTitle(), e);
            return ChunkingResult.error("Chunking failed: " + e.getMessage());
        }
    }

    @Override
    public String getChunkAsBase64(Long songId, Integer chunkIndex) {
        try {
            Optional<SongChunk> chunkOpt = songChunkRepository.findBySongInfoIdAndChunkIndex(songId, chunkIndex);
            if (chunkOpt.isEmpty()) {
                log.warn("Chunk not found: songId={}, chunkIndex={}", songId, chunkIndex);
                return null;
            }

            SongChunk chunk = chunkOpt.get();
            Path chunkPath = Paths.get(chunk.getChunkUrl());

            if (!Files.exists(chunkPath)) {
                log.error("Chunk file not found: {}", chunkPath);
                return null;
            }

            byte[] chunkData = Files.readAllBytes(chunkPath);
            return Base64.getEncoder().encodeToString(chunkData);

        } catch (Exception e) {
            log.error("Error reading chunk: songId={}, chunkIndex={}", songId, chunkIndex, e);
            return null;
        }
    }

    @Override
    public Long getChunkCount(Long songId) {
        return songChunkRepository.countBySongInfoId(songId);
    }

    @Override
    public List<SongChunk> getChunksMetadata(Long songId) {
        return songChunkRepository.findBySongInfoIdOrderByChunkIndex(songId);
    }

    @Override
    @Transactional
    public void deleteChunks(Long songId) {
        try {
            List<SongChunk> chunks = songChunkRepository.findBySongInfoIdOrderByChunkIndex(songId);

            // Delete chunk files
            for (SongChunk chunk : chunks) {
                try {
                    Files.deleteIfExists(Paths.get(chunk.getChunkUrl()));
                } catch (IOException e) {
                    log.warn("Failed to delete chunk file: {}", chunk.getChunkUrl(), e);
                }
            }

            // Delete chunks directory
            Path chunksDir = Paths.get(CHUNKS_DIRECTORY + songId);
            deleteDirectory(chunksDir);

            // Delete chunk records from database
            songChunkRepository.deleteBySongInfoId(songId);

            log.info("Deleted all chunks for song ID: {}", songId);
        } catch (Exception e) {
            log.error("Error deleting chunks for song ID: {}", songId, e);
        }
    }

    private void deleteDirectory(Path directory) {
        try {
            if (Files.exists(directory)) {
                Files.walk(directory)
                    .sorted((p1, p2) -> p2.toString().length() - p1.toString().length()) // Delete files before directories
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            log.warn("Failed to delete: {}", path, e);
                        }
                    });
            }
        } catch (IOException e) {
            log.warn("Failed to delete directory: {}", directory, e);
        }
    }
}

