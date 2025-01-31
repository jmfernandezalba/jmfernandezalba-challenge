package com.crossmint.challenge.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.NonNull;

public abstract class AstralObject {

    @NonNull
    @JsonIgnore
    private final SpaceCell spaceCell;

    public AstralObject(@NonNull SpaceCell spaceCell) {
        this.spaceCell = spaceCell;
    }

    @SuppressWarnings("unused")
    public @NonNull String getCandidateId() {
        return spaceCell.getMegaverse().candidateId();
    }

    @SuppressWarnings("unused")
    public int getRow() {
        return spaceCell.getRow();
    }

    @SuppressWarnings("unused")
    public int getColumn() {
        return spaceCell.getColumn();
    }

    public abstract @NonNull String endpoint();

    @Override
    public abstract String toString();
}
