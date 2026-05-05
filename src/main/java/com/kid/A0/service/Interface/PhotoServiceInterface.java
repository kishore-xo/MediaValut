package com.kid.A0.service.Interface;

import com.kid.A0.dto.MediaResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

public interface PhotoServiceInterface {
    MediaResponse postPhoto(Long userId, MultipartFile photo) throws IOException;

    void deletePhoto(long userId, String photoId);

    ResponseEntity<Resource> getPhoto(Long userId, String photoId);

    List<MediaResponse> getPhotos(Principal principal);

    void deleteAll();
}
