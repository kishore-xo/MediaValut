package com.kid.A0.repo;

import com.kid.A0.model.Media;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MediaRepo extends JpaRepository<Media, String> {
    List<Media> findMediaByTypeEqualsAndIsDeleted(String type, boolean isDeleted);

    Optional<Media> findByIdAndTypeAndIsDeleted(String id, String type, boolean isDeleted);

    List<Media> findMediaByUserIdAndTypeAndIsDeleted(Long userId, String type, boolean isDeleted);

    int countByUserIdAndTypeAndIsDeletedFalse(Long userId, String type);

}
