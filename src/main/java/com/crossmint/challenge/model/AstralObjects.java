package com.crossmint.challenge.model;

/**
 * The {@code AstralObjects} class provides utility methods for working with various types of astral objects,
 * such as Polyanets, Sooloons, and Comeths, within a given {@link SpaceCell}.
 * It includes methods to parse string representations of astral objects
 * and create corresponding object instances with appropriate attributes.
 */
public class AstralObjects {

    /**
     * Creates an {@link AstralObject} instance based on a given string representation and its associated {@link SpaceCell}.
     * The string input is parsed to determine the type of the object and its attributes.
     *
     * @param objectStr a string representation of the astral object that specifies its type and,
     *                  optionally, additional attributes such as direction or color.
     *                  Example formats include "POLYANET", "RED_SOLOON", "UP_COMETH".
     * @param spaceCell the {@link SpaceCell} in which the resulting {@link AstralObject} resides.
     *                  This cell provides positional and contextual information related to the object.
     * @return an instance of an {@link AstralObject} corresponding to the input string,
     * or {@code null} if the input specifies a "SPACE" object.
     * @throws IllegalArgumentException if the input string does not match any recognized format
     *                                  or contains invalid attributes.
     */
    public static AstralObject fromString(String objectStr, SpaceCell spaceCell) {

        String[] objectParts = objectStr.split("_");
        return switch (objectParts.length) {
            case 1 -> switch (objectStr) {
                case SpaceCell.OBJECT_NAME -> null;
                case Polyanet.OBJECT_NAME -> new Polyanet(spaceCell);
                default -> throw new IllegalArgumentException("Unexpected value: " + objectStr);
            };
            case 2 -> switch (objectParts[1]) {
                case Soloon.OBJECT_NAME -> new Soloon(spaceCell, Soloon.Color.valueOf(objectParts[0].toLowerCase()));
                case Cometh.OBJECT_NAME -> new Cometh(spaceCell, Cometh.Direction.valueOf(objectParts[0].toLowerCase()));
                default -> throw new IllegalArgumentException("Unexpected value: " + objectStr);
            };
            default -> throw new IllegalArgumentException("Unexpected value: " + objectStr);
        };
    }
}
