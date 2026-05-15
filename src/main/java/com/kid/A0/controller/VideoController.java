package com.kid.A0.controller;

import com.kid.A0.annotation.CheckUploadLimit;
import com.kid.A0.annotation.RateLimit;
import com.kid.A0.dto.MediaResponse;
import com.kid.A0.service.VideoService;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/video")
@RateLimit
public class VideoController {

    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @CheckUploadLimit(type = "video")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MediaResponse> postVideo(Principal principal, @RequestParam("video") MultipartFile video) throws IOException {
        return ResponseEntity.ok()
                .body(videoService.postVideo(principal.getName(), video));
    }

    @DeleteMapping("/{videoId}")
    public ResponseEntity<Void> deleteVideo(Principal principal, @PathVariable String videoId) {
        videoService.deleteVideo(principal.getName(), videoId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{videoId}")
    public ResponseEntity<ResourceRegion> getVideo(@RequestParam(required = false) String quality,
                                                   Principal principal,
                                                   @PathVariable String videoId,
                                                   @RequestHeader HttpHeaders headers) throws IOException {
        return videoService.getVideo(principal.getName(), videoId, quality, headers);
    }

    @GetMapping("/{videoId}/qualities")
    public ResponseEntity<List<String>> getVideoQualities(Principal principal,
                                                          @PathVariable String videoId) throws IOException {
        return ResponseEntity.ok(videoService.getVideoQualities(principal.getName(), videoId));
    }

    @GetMapping("/{videoId}/status")
    public ResponseEntity<MediaResponse> getVideoStatus(Principal principal,
                                                        @PathVariable String videoId) throws IOException {
        return ResponseEntity.ok(videoService.getVideoStatus(principal.getName(), videoId));
    }

    @GetMapping
    public ResponseEntity<List<MediaResponse>> getVideos(Principal principal) {
        return ResponseEntity.ok(videoService.getVideos(principal.getName()));
    }

}
