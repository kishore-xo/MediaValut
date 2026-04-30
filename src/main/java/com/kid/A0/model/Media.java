package com.kid.A0.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "media",indexes = {
        @Index(name = "idx_media_user_type_deleted", columnList = "userId ,type,is_deleted"),
        @Index(name = "idx_media_id_type_deleted", columnList = "id ,type,is_deleted")
})
public class Media implements Serializable {
    @Id
    private String id;
    private String filePath;
    private String title;
    private Long userId;
    private String type;
    private boolean isDeleted;
    @Enumerated(EnumType.STRING)
    private VideoStage stage;

    @OneToMany(mappedBy = "media",cascade = CascadeType.ALL,orphanRemoval = true)
    private List<MediaVersion> mediaVersionList;
}

