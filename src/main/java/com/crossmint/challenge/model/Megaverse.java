package com.crossmint.challenge.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.NonNull;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Represents the entire megaverse structure, consisting of a two-dimensional array of {@link SpaceCell} instances.
 * Each {@link SpaceCell} contains positional and contextual information about astral entities within the megaverse.
 * The Megaverse is uniquely identified by a candidate ID.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record Megaverse(@NonNull String candidateId, @NonNull SpaceCell[][] spaceCells) {

    @Override
    public String toString() {
        return "Megaverse{\ncandidateId=" + candidateId + ",\nspaceCells=[\n" + Arrays.stream(spaceCells)
            .map(Arrays::toString)
            .collect(Collectors.joining(",\n")) + "\n]\n}";
    }
}
