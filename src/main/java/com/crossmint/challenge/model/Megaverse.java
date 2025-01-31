package com.crossmint.challenge.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.NonNull;

import java.util.Arrays;
import java.util.stream.Collectors;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record Megaverse(@NonNull String candidateId, @NonNull SpaceCell[][] spaceCells) {

    @Override
    public String toString() {
        return "Megaverse{\ncandidateId=" + candidateId + ",\nspaceCells=[\n" + Arrays.stream(spaceCells)
            .map(Arrays::toString)
            .collect(Collectors.joining(",\n")) + "\n]\n}";
    }
}
