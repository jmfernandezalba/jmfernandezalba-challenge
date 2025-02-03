package com.crossmint.challenge.model;

import lombok.Getter;

@Getter
public class Soloon extends AstralObject {

    /**
     * A constant representing the name of the Soloon object type.
     * This is used to uniquely identify Soloon objects in the system.
     * The value is "SOLOON".
     */
    public static final String OBJECT_NAME = "SOLOON";

    public enum Color {blue, red, purple, white}

    private final Color color;

    public Soloon(SpaceCell spaceCell, Color color) {
        super(spaceCell);
        this.color = color;
    }

    @Override
    public String endpoint() {
        return "soloons";
    }

    @Override
    public String toString() {
        return String.format("%-13s", color.toString().toUpperCase() + "_" + OBJECT_NAME);
    }
}
