package com.crossmint.challenge.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.NonNull;

/**
 * The {@code AstralObject} class represents an abstract model for various astral entities
 * that exist within a {@link SpaceCell}. This serves as a base class to define common
 * behavior and properties for specific astral objects, such as Polyanets, Sooloons, and Comeths.
 * Each astral object is associated with a specific {@link SpaceCell} instance and
 * requires subclasses to implement specific properties and methods.
 */
public abstract class AstralObject {

    @NonNull
    @JsonIgnore
    private final SpaceCell spaceCell;

    public AstralObject(@NonNull SpaceCell spaceCell) {
        this.spaceCell = spaceCell;
    }

    //These methods are actually being used by the json mapper
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
