package com.kid.A0.dto;

import com.kid.A0.model.Media;
import com.kid.A0.model.VideoStage;

import java.io.Serializable;

public record MediaResponse
        (
                String mediaId,
                String mediaTitle,
                Long userId,
                String type,
                VideoStage stage,
                boolean isDeleted) implements Serializable {

    public MediaResponse(Media media) {
        this(media.getId(), media.getTitle(), media.getUserId(), media.getType(), media.getStage(), media.isDeleted());
    }
}
