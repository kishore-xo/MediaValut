package com.kid.A0.service.Interface;

import com.kid.A0.dto.MediaResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface PhotoServiceInterface {
    MediaResponse postPhoto(String username, MultipartFile photo) throws IOException;

    void deletePhoto(String username, String photoId);

    ResponseEntity<Resource> getPhoto(String username, String photoId);

    List<MediaResponse> getPhotos(String username);

    void deleteAll();
}
