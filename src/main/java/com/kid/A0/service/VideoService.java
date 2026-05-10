package com.kid.A0.service;

import com.kid.A0.dto.MediaResponse;
import com.kid.A0.exception.VideoNotFoundException;
import com.kid.A0.model.*;
import com.kid.A0.repo.MediaRepo;
import com.kid.A0.repo.MediaVersionRepo;
import com.kid.A0.repo.UserRepo;
import com.kid.A0.service.Interface.VideoServiceInterface;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class VideoService implements VideoServiceInterface {

    private final MediaRepo mediaRepo;
    private final UserRepo userRepo;
    private final VideoFixService videoFixService;
    private final MediaVersionRepo mediaVersionRepo;

    @Value("${video.Path}")
    private String path;

    public VideoService(MediaRepo mediaRepo, UserRepo userRepo, VideoFixService videoFixService,
            MediaVersionRepo mediaVersionRepo) {
        this.mediaRepo = mediaRepo;
        this.userRepo = userRepo;
        this.videoFixService = videoFixService;
        this.mediaVersionRepo = mediaVersionRepo;
    }

    @Transactional
    public MediaResponse postVideo(Long userId, MultipartFile uploadVideo) throws IOException {

        if (uploadVideo.isEmpty())
            throw new IllegalArgumentException("File Is Empty");

        if (!userRepo.existsUserById(userId)) {
            throw new AccessDeniedException("Unauthorized");
        }
        handleVideoSize(userId, uploadVideo);
        String contentType = uploadVideo.getContentType();

        if (contentType == null || !contentType.startsWith("video/")) {
            throw new IllegalArgumentException("File Is Not A Valid Video Format");
        }

        String fileId = UUID.randomUUID().toString();
        String fileName = fileId + ".mp4";
        if (!Files.exists(Path.of(path))) {
            Files.createDirectory(Path.of(path));
        }
        Path uploadPath = Path.of(path).resolve(fileName);

        Files.copy(uploadVideo.getInputStream(), uploadPath, StandardCopyOption.REPLACE_EXISTING);

        Media video = Media.builder()
                .id(fileId)
                .filePath(uploadPath.toString())
                .title(uploadVideo.getOriginalFilename())
                .userId(userId)
                .type("video")
                .stage(VideoStage.PROCESSING)
                .isDeleted(false)
                .build();
        mediaRepo.save(video);

        Runnable triggerConversion = () -> videoFixService.startVideoProcessingChain(uploadPath.toString(), fileId);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    triggerConversion.run();
                }
            });
        } else {
            triggerConversion.run();
        }
        return new MediaResponse(video);
    }

    @Transactional
    public void deleteVideo(Principal principal, String videoId) {
        Long userId = Long.valueOf(principal.getName());
        Media video = mediaRepo.findByIdAndTypeAndIsDeleted(videoId, "video", false)
                .orElseThrow(() -> new VideoNotFoundException("Video Not Found"));
        // if (!video.getUserId().equals(userId)) {
        //     throw new RuntimeException("Unauthorized");
        // }
        video.setDeleted(true);
        mediaRepo.save(video);
    }

    public ResponseEntity<ResourceRegion> getVideo(Long userId, String videoId, String quality, HttpHeaders headers)
            throws IOException {

        Media video = mediaRepo.findByIdAndTypeAndIsDeleted(videoId, "video", false)
                .orElseThrow(() -> new VideoNotFoundException("Video Not Found"));
        // if (!userId.equals(video.getUserId()))
        //     throw new AccessDeniedException("Unauthorized User");
        
        // if (VideoStage.PROCESSING.equals(video.getStage())) {
        //     throw new RuntimeException("Video Upload Process Not Completed Yet");
        // }

        String filePath = video.getFilePath();
        if (quality != null && !quality.isBlank() && !quality.equalsIgnoreCase("source")) {
            String normalizedQuality = normalizeQuality(quality);
            filePath = mediaVersionRepo.findByMediaAndQuality(video, normalizedQuality)
                    .map(MediaVersion::getFilePath)
                    .orElseThrow(() -> new RuntimeException("Requested quality not found: " + normalizedQuality));
        }

        UrlResource resource = new UrlResource("file:" + filePath);
        long contentLength = resource.contentLength();

        HttpRange range = headers.getRange().stream().findFirst().orElse(null);
        ResourceRegion resourceRegion;

        if (range != null) {
            long start = range.getRangeStart(contentLength);
            long end = range.getRangeEnd(contentLength);
            long rangeLength = Math.min(1024 * 1024L, end - start + 1);
            resourceRegion = new ResourceRegion(resource, start, rangeLength);
            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .contentType(MediaTypeFactory.getMediaType(resource)
                            .orElse(MediaType.valueOf("video/mp4")))
                    .body(resourceRegion);
        } else {
            resourceRegion = new ResourceRegion(resource, 0, contentLength);
            return ResponseEntity.status(HttpStatus.OK)
                    .contentType(MediaTypeFactory.getMediaType(resource)
                            .orElse(MediaType.valueOf("video/mp4")))
                    .body(resourceRegion);
        }

    }

    public List<String> getVideoQualities(Long userId, String videoId) throws IOException {
        Media video = mediaRepo.findByIdAndTypeAndIsDeleted(videoId, "video", false)
                .orElseThrow(() -> new VideoNotFoundException("Video Not Found"));
        if (!userId.equals(video.getUserId()))
            throw new AccessDeniedException("Unauthorized User");

        return mediaVersionRepo.findByMediaOrderByQualityAsc(video)
                .stream()
                .map(MediaVersion::getQuality)
                .sorted(Comparator.comparingInt(this::extractQualityNumber))
                .toList();
    }

    public MediaResponse getVideoStatus(Long userId, String videoId) throws IOException {
        Media video = mediaRepo.findByIdAndTypeAndIsDeleted(videoId, "video", false)
                .orElseThrow(() -> new VideoNotFoundException("Video Not Found"));
        if (!userId.equals(video.getUserId()))
            throw new AccessDeniedException("Unauthorized User");
        return new MediaResponse(video);
    }

    public List<MediaResponse> getVideos(Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        if (!userRepo.existsUserById(userId)) {
            throw new UsernameNotFoundException("User Not Found");
        }

        return mediaRepo
                .findMediaByUserIdAndTypeAndIsDeleted(userId, "video", false)
                .stream()
                .map(MediaResponse::new)
                .toList();
    }

    // this is done by scheduling
    @Transactional
    public void deleteAll() {
        List<Media> videos = mediaRepo.findMediaByTypeEqualsAndIsDeleted("video", true);

        for (Media video : videos) {
            List<MediaVersion> mediaVersions = mediaVersionRepo.findByMedia(video);
            for (MediaVersion version : mediaVersions) {
                deleteFile(version.getFilePath());
            }
            mediaVersionRepo.deleteAll(mediaVersions);
            deleteFile(video.getFilePath());
        }
        mediaRepo.deleteAll(videos);
    }

    private void deleteFile(String path) {
        try {
            if (path != null) {
                Files.deleteIfExists(Path.of(path));
            }
        } catch (IOException e) {
            log.error("Failed to delete physical file at: {}", path);
        }
    }

    private String normalizeQuality(String quality) {
        String trimmed = quality.trim().toLowerCase();
        if (trimmed.endsWith("p")) {
            return trimmed;
        }
        return trimmed + "p";
    }

    private int extractQualityNumber(String quality) {
        String digits = quality == null ? "" : quality.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            return Integer.MAX_VALUE;
        }
        return Integer.parseInt(digits);
    }

    private void handleVideoSize(Long userid, MultipartFile uploadVideo) {
        User user = userRepo.findByIdWithPlan(userid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User Not Found"));
        if (user.getSubscription() == null || user.getSubscription().getPlan() == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "User has no active plan configuration");
        }
        Plan plan = user.getSubscription().getPlan();
        DataSize videoSize = DataSize.parse(plan.getVideoSize());
        if (uploadVideo.getSize() > videoSize.toBytes()) {
            throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE,
                    String.format("video size exceeds your %s plan limit of %s",
                            plan.getName(), plan.getVideoSize()));
        }
    }
}
