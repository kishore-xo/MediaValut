package com.kid.A0.schedule;

import com.kid.A0.repo.ApiKeyRepo;
import com.kid.A0.service.PhotoService;
import com.kid.A0.service.VideoService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class Schedule {

    private final ApiKeyRepo apiKeyRepo;
    private final VideoService videoService;
    private final PhotoService photoService;

    public Schedule(ApiKeyRepo apiKeyRepo, VideoService videoService, PhotoService photoService) {
        this.apiKeyRepo = apiKeyRepo;
        this.videoService = videoService;
        this.photoService = photoService;
    }

    @Scheduled(fixedDelay = 600000)
    public void deleteRevokedApi() {
        apiKeyRepo.deleteApiKeyByRevoked(true);
    }

    @Scheduled(fixedDelay = 600000)
    public void deleteVideo() {
        videoService.deleteAll();
        photoService.deleteAll();
    }
}
