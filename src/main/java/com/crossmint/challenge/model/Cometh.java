package com.crossmint.challenge.model;

import lombok.Getter;

@Getter
public class Cometh extends AstralObject {

    /**
     * A constant representing the name of the Cometh object type.
     * This is used to uniquely identify Cometh objects in the system.
     * The value is "COMETH".
     */
    public static final String OBJECT_NAME = "COMETH";

    public enum Direction {up, down, right, left}

    private final Direction direction;

    public Cometh(SpaceCell spaceCell, Direction direction) {
        super(spaceCell);
        this.direction = direction;
    }

    @Override
    public String endpoint() {
        return "comeths";
    }

    public String toString() {
        return String.format("%-13s", direction + "_" + OBJECT_NAME);
    }
}
