package com.crossmint.challenge.main;

import com.crossmint.challenge.connectors.MegaverseConnection;
import com.crossmint.challenge.model.Megaverse;

import java.io.IOException;

public class Main {

    public static final String CANDIDATE_ID = "87a965e7-007b-434d-97e1-30aad508402e";

    public static void main(String[] args) {

        long startTime = System.currentTimeMillis();

        String candidateId = System.getenv("CANDIDATE_ID");
        if (candidateId == null) {
            candidateId = CANDIDATE_ID;
        }

        try {
            MegaverseConnection connection = new MegaverseConnection(candidateId);
            Megaverse megaverse = connection.readGoal();
            System.out.println(megaverse);
            connection.publishState(megaverse);
            System.out.println("Megaverse published successfully.");
        } catch (IOException e) {
            System.err.println("Failed to execute the challenge: " + e.getLocalizedMessage());
        }

        long endTime = System.currentTimeMillis();
        double elapsedTimeInSeconds = (endTime - startTime) / 1000.0;
        System.out.println("Elapsed time: " + elapsedTimeInSeconds + " seconds.");
    }
}
