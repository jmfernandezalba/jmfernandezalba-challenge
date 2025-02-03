package com.crossmint.challenge.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AstralObjectsTest {

    @Test
    public void testFromStringWithAllCases() {
        // Arrange: Define the input strings, expected results, and a mock SpaceCell.
        Megaverse mockMegaverse = new Megaverse("testCandidate", new SpaceCell[0][0]);
        SpaceCell mockSpaceCell = new SpaceCell(mockMegaverse, 0, 0);

        String polyanetInput = "POLYANET";
        String redSoloonInput = "RED_SOLOON";
        String upComethInput = "UP_COMETH";
        String spaceInput = "SPACE";
        String invalidInput = "INVALID_INPUT";

        // Act & Assert: Test "POLYANET" input
        AstralObject resultPolyanet = AstralObjects.fromString(polyanetInput, mockSpaceCell);
        assertThat(resultPolyanet).isInstanceOf(Polyanet.class);
        //noinspection DataFlowIssue
        assertThat(resultPolyanet.toString()).isEqualTo("POLYANET     ");

        // Act & Assert: Test "RED_SOLOON" input
        AstralObject resultRedSoloon = AstralObjects.fromString(redSoloonInput, mockSpaceCell);
        assertThat(resultRedSoloon).isInstanceOf(Soloon.class);
        //noinspection DataFlowIssue
        assertThat(((Soloon) resultRedSoloon).getColor()).isEqualTo(Soloon.Color.red);
        assertThat(resultRedSoloon.toString()).isEqualTo("RED_SOLOON   ");

        // Act & Assert: Test "UP_COMETH" input
        AstralObject resultUpCometh = AstralObjects.fromString(upComethInput, mockSpaceCell);
        assertThat(resultUpCometh).isInstanceOf(Cometh.class);
        //noinspection DataFlowIssue
        assertThat(((Cometh) resultUpCometh).getDirection()).isEqualTo(Cometh.Direction.up);
        assertThat(resultUpCometh.toString()).isEqualTo("UP_COMETH    ");

        // Act & Assert: Test "SPACE" input
        AstralObject resultSpace = AstralObjects.fromString(spaceInput, mockSpaceCell);
        assertThat(resultSpace).isNull();

        // Act & Assert: Test invalid input
        assertThatThrownBy(() -> AstralObjects.fromString(invalidInput, mockSpaceCell))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unexpected value");
    }
}
