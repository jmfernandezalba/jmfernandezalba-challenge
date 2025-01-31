package com.crossmint.challenge.model;

public class Polyanet extends AstralObject {

    public Polyanet(SpaceCell spaceCell) {
        super(spaceCell);
    }

    @Override
    public String endpoint() {
        return "polyanets";
    }

    @Override
    public String toString() {
        return "POLYANET";
    }
}
