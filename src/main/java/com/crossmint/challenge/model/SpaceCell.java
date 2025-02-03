package com.crossmint.challenge.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NonNull;

import java.util.Optional;

/**
 * Represents a single cell within a two-dimensional megaverse structure.
 * Each SpaceCell holds positional information (row and column) and can optionally contain an astral object.
 * SpaceCells are contextually tied to a specific {@link Megaverse}.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class SpaceCell {

    /**
     * A constant representing the name of the "SPACE" object type.
     * This is used to uniquely identify empty or unoccupied SpaceCells within the system.
     * The value is "SPACE".
     */
    public static final String OBJECT_NAME = "SPACE";

    @NonNull
    @JsonIgnore
    private final Megaverse megaverse;
    private final int row;
    private final int column;
    private AstralObject astralObject;

    public SpaceCell(@NonNull Megaverse megaverse, int row, int column) {
        this.megaverse = megaverse;
        this.row = row;
        this.column = column;
    }

    public SpaceCell fillFromString(String objectStr) {
        astralObject = AstralObjects.fromString(objectStr, this);
        return this;
    }

    public Optional<AstralObject> getAstralObject() {
        return Optional.ofNullable(astralObject);
    }

    public String toString() {
        if (astralObject != null) {
            return astralObject.toString();
        } else {
            return String.format("%-13s", OBJECT_NAME);
        }
    }
}
