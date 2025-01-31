package com.crossmint.challenge.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NonNull;

import java.util.Optional;

@Getter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class SpaceCell {

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
        switch (objectStr) {
            case "SPACE" -> astralObject = null;
            case "POLYANET" -> astralObject = new Polyanet(this);
            default -> throw new IllegalArgumentException("Unexpected value: " + objectStr);
        }
        return this;
    }

    public Optional<AstralObject> getAstralObject() {
        return Optional.ofNullable(astralObject);
    }

    public String toString() {
        if (astralObject != null) {
            return astralObject.toString();
        } else {
            return "_SPACE__";
        }
    }
}
