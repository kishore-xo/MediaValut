package com.kid.A0.service.Interface;

import com.kid.A0.dto.MediaResponse;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface PhotoServiceInterface {
    MediaResponse postPhoto(String username, MultipartFile photo) throws IOException;

    void deletePhoto(String username, String photoId);

    ResponseEntity<Resource> getPhoto(String username, String photoId);

    Page<MediaResponse> getPhotos(String username, Pageable pageable);

    void deleteAll();
}
