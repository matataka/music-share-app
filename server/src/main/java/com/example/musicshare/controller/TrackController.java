package com.example.musicshare.controller;

import com.example.musicshare.model.Track;
import com.example.musicshare.repository.TrackRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import jakarta.annotation.PostConstruct;
import com.mpatric.mp3agic.Mp3File;

@RestController
@RequestMapping("/api/tracks")
public class TrackController {
    private final TrackRepository trackRepository;
    @Value("${music.upload.dir:uploads}")
    private String uploadDir;

    public TrackController(TrackRepository trackRepository) {
        this.trackRepository = trackRepository;
    }

    @PostConstruct
    public void initUploadDir() {
        if (uploadDir != null && !uploadDir.isEmpty()) {
            new File(uploadDir).mkdirs();
        }
    }

    // 曲一覧取得
    @GetMapping
    public List<Track> getTracks() {
        return trackRepository.findAll();
    }

    // 曲アップロード
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Track uploadTrack(@RequestParam("file") MultipartFile file) throws IOException {
        String filename = System.currentTimeMillis() + "_" + StringUtils.cleanPath(file.getOriginalFilename());
        Path filePath = Paths.get(uploadDir, filename);
        Files.write(filePath, file.getBytes());

        Track track = new Track();
        track.setTitle(file.getOriginalFilename());
        
        // MP3ファイルの場合、実際の長さを取得
        if (file.getOriginalFilename().toLowerCase().endsWith(".mp3")) {
            try {
                Mp3File mp3file = new Mp3File(filePath);
                long lengthInSeconds = mp3file.getLengthInSeconds();
                track.setLengthMinutes((int) Math.ceil(lengthInSeconds / 60.0));
            } catch (Exception e) {
                // MP3解析エラー時は1分とする
                track.setLengthMinutes(1);
            }
        } else {
            // MP3以外は1分とする
            track.setLengthMinutes(1);
        }

        track.setFilePath(filePath.toString());
        return trackRepository.save(track);
    }

    // 曲再生（ファイル取得）
    @GetMapping("/{id}/play")
    public ResponseEntity<byte[]> playTrack(@PathVariable("id") Long id) throws IOException {
        Optional<Track> opt = trackRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Track track = opt.get();
        File file = new File(track.getFilePath());
        if (!file.exists()) return ResponseEntity.notFound().build();
        byte[] data = Files.readAllBytes(file.toPath());
        String mimeType = Files.probeContentType(file.toPath());
        if (mimeType == null) mimeType = "application/octet-stream";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + track.getTitle())
                .contentType(MediaType.parseMediaType(mimeType))
                .body(data);
    }

    // 曲削除
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTrack(@PathVariable("id") Long id) {
        Optional<Track> opt = trackRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Track track = opt.get();
        // ファイルを削除
        try {
            Files.deleteIfExists(Paths.get(track.getFilePath()));
        } catch (IOException e) {
            // ファイル削除に失敗しても、DBからは削除する
        }

        // DBから削除
        trackRepository.delete(track);
        return ResponseEntity.ok().build();
    }
}
