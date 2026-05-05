package com.kid.A0.service.Interface;

import com.kid.A0.dto.MediaResponse;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

public interface VideoServiceInterface {

	MediaResponse postVideo(Long userId, MultipartFile uploadVideo) throws IOException;

	void deleteVideo(Principal principal, String videoId);

	ResponseEntity<ResourceRegion> getVideo(Long userId, String videoId, String quality, HttpHeaders headers) throws IOException;

	List<String> getVideoQualities(Long userId, String videoId) throws IOException;

	MediaResponse getVideoStatus(Long userId, String videoId) throws IOException;

	List<MediaResponse> getVideos(Principal principal);

	void deleteAll();
}
