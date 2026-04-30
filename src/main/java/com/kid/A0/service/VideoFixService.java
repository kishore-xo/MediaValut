package com.kid.A0.service;

import com.kid.A0.model.MediaVersion;
import com.kid.A0.model.VideoStage;
import com.kid.A0.repo.MediaRepo;
import com.kid.A0.repo.MediaVersionRepo;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class VideoFixService {

    private final MediaRepo mediaRepo;
    private final MediaVersionRepo mediaVersionRepo;

    @Value("${video.ffmpeg}")
    private String ffmpegPath;

    @Value("${video.ffprobe}")
    private String ffprobePath;

    @Value("${video.Path}")
    private Path filePath;

    public VideoFixService(MediaRepo mediaRepo, MediaVersionRepo mediaVersionRepo) {
        this.mediaRepo = mediaRepo;
        this.mediaVersionRepo = mediaVersionRepo;
    }

    @Async("videoExecutor")
    public void startVideoProcessingChain(String originalPath, String videoId) {
        try {
            log.info("Starting High-Quality Chain for {}", videoId);

            // 1. Get the height of the original video to prevent upscaling
            int originalHeight = getOriginalHeight(originalPath);
            log.info("Original video height: {}p", originalHeight);

            // 2. Create the "Web Friendly" master version from Original
            String webPath = originalPath.replace(".mp4", "_web.mp4");
            int webExit = runCompatibilityEncode(originalPath, webPath);
            boolean webReady = false;

            if (webExit == 0) {
                mediaRepo.findById(videoId).ifPresent(media -> {
                    media.setFilePath(webPath);
                    mediaRepo.save(media);
                });
                log.info("Step 1: Web Format Complete");
                webReady = true;
            } else {
                log.error("Step 1 failed for {}. Keeping original source path to avoid broken playback.", videoId);
            }

            // 3. Generate Splits from the ORIGINAL for best quality
            generateQualitySplits(originalPath, videoId, originalHeight);

            // 4. Cleanup the temporary raw upload
            if (webReady) {
                Files.deleteIfExists(Path.of(originalPath));
            }
            log.info("Step 3: Cleanup complete. Processing Finished for {}", videoId);
            mediaRepo.findById(videoId).ifPresent(media -> {
                media.setStage(VideoStage.COMPLETED);
                mediaRepo.save(media);
            });

        } catch (Exception e) {
            log.error("Processing Chain failed for {}", videoId, e);
        }
    }

    private void generateQualitySplits(String sourcePath, String videoId, int maxAllowedHeight) {
        // Full resolution list from 144p to 8K
        String[] resolutions = {"360", "480", "720", "1080", "1440", "2160", "4320"};

        var media = mediaRepo.findById(videoId).orElse(null);
        if (media == null) {
            log.error("Media {} not found. Split generation skipped.", videoId);
            return;
        }

        for (String r : resolutions) {
            int targetHeight = Integer.parseInt(r);

            // ONLY split if the original is high enough quality (No Upscaling)
            if (targetHeight > maxAllowedHeight) {
                log.info("Skipping {}p (Target higher than source {}p)", r, maxAllowedHeight);
                continue;
            }

            Path outputDir = filePath.resolve(r + "p");
            Path outputPath = outputDir.resolve(videoId + "_" + r + "p.mp4");

            try {
                Files.createDirectories(outputDir);

                ProcessBuilder pb = getProcessBuilder(sourcePath, r, outputPath);
                pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
                int exit = pb.start().waitFor();

                if (exit == 0) {
                    MediaVersion version = new MediaVersion();
                    version.setQuality(r + "p");
                    version.setFilePath(outputPath.toString());
                    version.setMedia(media);
                    mediaVersionRepo.save(version);
                    log.info("Generated and saved {}p version", r);
                }
            } catch (Exception e) {
                log.error("Failed split for {}p: {}", r, e.getMessage());
            }
        }
    }

    private @NonNull ProcessBuilder getProcessBuilder(String sourcePath, String r, Path outputPath) {
        ProcessBuilder pb = new ProcessBuilder(
                ffmpegPath, "-y", "-i", sourcePath,
                "-vf", "scale=-2:" + r,
                "-c:v", "libx264", "-profile:v", "main", "-level:v", "4.0",
                "-pix_fmt", "yuv420p", "-movflags", "+faststart",
                "-preset", "ultrafast",
                "-crf", "23", // HQ CRF
                "-c:a", "aac", "-profile:a", "aac_low", "-b:a", "128k", "-ar", "48000", "-ac", "2",
                outputPath.toString()
        );

        pb.redirectErrorStream(true);
        return pb;
    }

    private int runCompatibilityEncode(String videoPath, String fixPath) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                ffmpegPath, "-y", "-i", videoPath,
                "-map", "0:v:0", "-map", "0:a:0?", "-sn", "-dn",
                "-c:v", "libx264",
                "-profile:v", "high",  // 1. Upgrade to High Profile for better detail
                "-level:v", "4.1",     // 2. Standard for 1080p@60fps
                "-pix_fmt", "yuv420p",
                "-preset", "medium",   // 3. 'medium' or 'slow' provides much better quality/size ratio
                "-crf", "18",          // 4. Change 22 -> 18 (This is the "Visually Lossless" target)
                "-movflags", "+faststart",
                "-c:a", "aac", "-b:a", "192k", // 5. Slightly higher audio bitrate for better sound
                "-ar", "48000", "-ac", "2",
                fixPath
        );
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        return pb.start().waitFor();
    }
    private int getOriginalHeight(String videoPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    ffprobePath, "-v", "error", "-select_streams", "v:0",
                    "-show_entries", "stream=height", "-of", "csv=p=0", videoPath
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                if (!process.waitFor(10, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                    throw new IOException("ffprobe timeout");
                }
                if (line != null && !line.isBlank()) return Integer.parseInt(line.trim());
            }
        } catch (Exception e) {
            log.error("Could not determine video height, defaulting to 720");
        }
        return 720;
    }
}