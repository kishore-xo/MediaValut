package com.kid.A0.controller;

import com.kid.A0.annotation.CheckUploadLimit;
import com.kid.A0.annotation.RateLimit;
import com.kid.A0.dto.MediaResponse;
import com.kid.A0.service.PhotoService;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;

@RestController
@RequestMapping("/api/v1/photo")
@RateLimit
public class PhotoController {

    private final PhotoService photoService;

    public PhotoController(PhotoService photoService) {
        this.photoService = photoService;
    }

    @CheckUploadLimit(type = "photo")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MediaResponse> postPhoto(Principal principal, @RequestParam("photo") MultipartFile photo) throws IOException {

        MediaResponse response = photoService.postPhoto(principal.getName(), photo);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    @DeleteMapping("/{photoId}")
    public ResponseEntity<?> deletePhoto(Principal principal, @PathVariable String photoId) {
        photoService.deletePhoto(principal.getName(), photoId);
        return ResponseEntity.status(HttpStatus.OK).body("Deleted");
    }

    @GetMapping("/{photoId}")
    public ResponseEntity<Resource> getPhoto(Principal principal, @PathVariable String photoId) {
        return photoService.getPhoto(principal.getName(), photoId);
    }

    @GetMapping
    public ResponseEntity<Page<MediaResponse>> getPhotos(Principal principal,
                                                         @PageableDefault(size = 5)Pageable pageable) {
        return ResponseEntity.ok(photoService.getPhotos(principal.getName(),pageable));
    }
}
