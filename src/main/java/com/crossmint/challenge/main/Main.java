package com.crossmint.challenge.main;

import com.crossmint.challenge.connectors.MegaverseConnection;
import com.crossmint.challenge.model.Megaverse;

import java.io.IOException;

public class Main {

    public static void main(String[] args) {

        try {
            MegaverseConnection connection = new MegaverseConnection("87a965e7-007b-434d-97e1-30aad508402e");
            Megaverse megaverse = connection.readGoal();
            System.out.println(megaverse);
            connection.publishState(megaverse);
            System.out.println("Megaverse published successfully.");
        } catch (IOException | InterruptedException e) {
            String errorMsg = "Failed to execute the challenge: " + e.getLocalizedMessage();
            System.err.println(errorMsg);
        }
    }
}
