package com.kid.A0.repo;

import com.kid.A0.model.Media;
import com.kid.A0.model.MediaVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MediaVersionRepo extends JpaRepository<MediaVersion,Long> {
    List<MediaVersion> findByMedia(Media media);

    Optional<MediaVersion> findByMediaAndQuality(Media media, String quality);

    List<MediaVersion> findByMediaOrderByQualityAsc(Media media);
}
