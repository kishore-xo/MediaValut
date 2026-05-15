package com.kid.A0.service.Interface;

import com.kid.A0.dto.MediaResponse;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface VideoServiceInterface {

	MediaResponse postVideo(String username, MultipartFile uploadVideo) throws IOException;

	void deleteVideo(String username, String videoId);

	ResponseEntity<ResourceRegion> getVideo(String username, String videoId, String quality, HttpHeaders headers) throws IOException;

	List<String> getVideoQualities(String username, String videoId) throws IOException;

	MediaResponse getVideoStatus(String username, String videoId) throws IOException;

	List<MediaResponse> getVideos(String username);

	void deleteAll();
}
