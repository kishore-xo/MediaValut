package com.kid.A0.service;

import com.kid.A0.dto.MediaResponse;
import com.kid.A0.exception.PhotoNotFoundException;
import com.kid.A0.model.Media;
import com.kid.A0.model.Plan;
import com.kid.A0.model.User;
import com.kid.A0.repo.MediaRepo;
import com.kid.A0.repo.UserRepo;
import com.kid.A0.service.Interface.PhotoServiceInterface;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
public class PhotoService implements PhotoServiceInterface {

    private final MediaRepo mediaRepo;
    private final UserRepo userRepo;

    @Value("${photoPath}")
    private String path;

    public PhotoService(MediaRepo mediaRepo, UserRepo userRepo) {
        this.mediaRepo = mediaRepo;
        this.userRepo = userRepo;
    }

    @Transactional
    public MediaResponse postPhoto(String username, MultipartFile photo) throws IOException {
        if (photo.isEmpty()) throw new IllegalArgumentException("File Is Empty");

        User user = currentUser(username);

        String contentType = photo.getContentType();

        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("File Is Not A Valid Image Format");
        }
        handlePhotoSize(user, photo);
        String extension = ".jpeg";
        String originalName = photo.getOriginalFilename();

        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf(".")).toLowerCase();
        }
        String photoId = UUID.randomUUID().toString();
        String photoName = photoId + extension;
        if (!Files.exists(Path.of(path))) {
            Files.createDirectory(Path.of(path));
        }
        Path uploadPath = Path.of(path).resolve(photoName);

        Files.copy(photo.getInputStream(), uploadPath, StandardCopyOption.REPLACE_EXISTING);


        Media photoResponse = Media.builder()
                .id(photoId)
                .filePath(uploadPath.toString())
                .title(photo.getOriginalFilename())
                .userId(user.getId())
                .type("photo")
                .isDeleted(false)
                .build();
        mediaRepo.save(photoResponse);
        return new MediaResponse(photoResponse);

    }

    @Transactional
    public void deletePhoto(String username, String photoId) {

        User user = currentUser(username);
        Media photo = mediaRepo.findByIdAndTypeAndIsDeleted(photoId, "photo", false)
                .orElseThrow(() -> new PhotoNotFoundException("Photo Not Found"));

        if (!photo.getUserId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }
        photo.setDeleted(true);
        mediaRepo.save(photo);
    }

    public void deleteAll() {
        List<Media> photos = mediaRepo.findMediaByTypeEqualsAndIsDeleted("photo", true);
        for (Media photo : photos) {
            try {
                Path filePath = Path.of(photo.getFilePath());
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                throw new RuntimeException("Failed to Delete Photo: " + photo.getId());
            }
        }
        mediaRepo.deleteAll(photos);
    }

    public ResponseEntity<Resource> getPhoto(String username, String photoId) {
        User user = currentUser(username);
        Media photo = mediaRepo.findByIdAndTypeAndIsDeleted(photoId, "photo", false)
                .orElseThrow(() -> new PhotoNotFoundException("Photo Not Found"));
//        if (!photo.getUserId().equals(user.getId())) {
//            throw new RuntimeException("Unauthorized");
//        }

        try {
            Path photoPath = Path.of(photo.getFilePath());
            Resource resource = new UrlResource(photoPath.toUri());

            if (resource.exists()) {
                return ResponseEntity.ok()
                        .contentType(MediaTypeFactory.getMediaType(resource)
                                .orElse(MediaType.IMAGE_JPEG))
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    public Page<MediaResponse> getPhotos(String username, Pageable pageable) {
        User user = currentUser(username);
        Page<MediaResponse> responses = mediaRepo
                .findMediaByUserIdAndTypeAndIsDeleted(user.getId(), "photo", false,pageable)
                .map(MediaResponse::new);
        return responses;
    }

    private User currentUser(String username) {
        return userRepo.findUserByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found"));
    }

    private void handlePhotoSize(User user, MultipartFile uploadPhoto) {
        Plan plan = user.getSubscription().getPlan();
        DataSize photoSize = DataSize.parse(plan.getPhotoSize());
        if (uploadPhoto.getSize() > photoSize.toBytes()) {
            throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE,
                    String.format("photo size exceeds your %s plan limit of %s",
                            plan.getName(), plan.getPhotoSize()));
        }
    }
}
