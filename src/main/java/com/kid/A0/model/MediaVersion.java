package com.kid.A0.model;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;

@Entity
@Data
@Table(name = "media_version",indexes = {
        @Index(name = "idx_version_media_quality",columnList = "mediaId, quality")
})
public class MediaVersion implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "media_id")
    private Media media;

    private String quality; // "480p", "720p"
    private String filePath;
}
