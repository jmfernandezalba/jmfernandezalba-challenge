package com.crossmint.challenge.model;

public class Polyanet extends AstralObject {

    /**
     * A constant representing the name of the Polyanet object type.
     * This is used to uniquely identify Polyanet objects in the system.
     * The value is "POLYANET".
     */
    public static final String OBJECT_NAME = "POLYANET";

    public Polyanet(SpaceCell spaceCell) {
        super(spaceCell);
    }

    @Override
    public String endpoint() {
        return "polyanets";
    }

    @Override
    public String toString() {
        return String.format("%-13s", OBJECT_NAME);
    }
}
