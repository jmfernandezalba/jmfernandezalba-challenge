package com.crossmint.challenge.connectors;

import com.crossmint.challenge.model.AstralObject;
import com.crossmint.challenge.model.Megaverse;
import com.crossmint.challenge.model.SpaceCell;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Handles the connection to the Megaverse API for a certain candidate.
 */
public class MegaverseConnection {

    public static final URI API_ROOT = URI.create("https://challenge.crossmint.io/api/");
    public static final String GOAL_ENDPOINT_FORMAT = "map/%s/goal";

    public static final int RETRY_DELAY_MS = 8000;

    @NonNull
    private final String candidateId;

    public MegaverseConnection(@NonNull String candidateId) {
        this.candidateId = candidateId;
    }

    /**
     * Publishes the provided astralObject to the Megaverse API using the specified client.
     *
     * @param httpClient   the HttpClient to send the request.
     * @param astralObject the AstralObject to publish.
     * @return the Future result of the request.
     */
    private CompletableFuture<HttpResponse<String>> publishWithRetry(HttpClient httpClient, AstralObject astralObject) {

        try {
            ObjectMapper jsonMapper = new ObjectMapper();
            String jsonBody = jsonMapper.writeValueAsString(astralObject);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(API_ROOT.resolve(astralObject.endpoint()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())

                // Fail the stage if the server responds with 429
                .whenCompleteAsync((response, _) -> {
                    if (response.statusCode() == 429) {
                        String errorMsg = "Too many requests: " + response + " -> " + response.body();
                        System.err.println(errorMsg);
                        throw new UncheckedIOException(new IOException(errorMsg));
                    }
                })

                // Retry once after certain delay if the stage failed
                .exceptionallyComposeAsync(
                    _ -> httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()),
                    CompletableFuture.delayedExecutor(RETRY_DELAY_MS, TimeUnit.MILLISECONDS))

                // Fail the stage if error the second time
                .whenCompleteAsync((response, _) -> {
                    if (response.statusCode() == 429) {
                        String errorMsg = "Too many requests: " + response + " -> " + response.body();
                        System.err.println(errorMsg);
                        throw new UncheckedIOException(new IOException(errorMsg));
                    }
                });

        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Could not parse astralObject: " + astralObject);
        }
    }

    /**
     * Publishes the state of the provided megaverse to the Megaverse API.
     *
     * @param megaverse the megaverse to publish.
     * @throws InterruptedException if some thread was interrupted while publishing the state.
     * @throws IOException          if there was some error while publishing the state.
     */
    public void publishState(@NonNull Megaverse megaverse) throws InterruptedException, IOException {

        LinkedList<CompletableFuture<HttpResponse<String>>> allPublishResults = new LinkedList<>();

        try (HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()) {

            for (SpaceCell[] row : megaverse.spaceCells()) {
                for (SpaceCell cell : row) {
                    cell.getAstralObject().ifPresent(
                        astralObject -> allPublishResults.add(publishWithRetry(httpClient, astralObject)));
                }
            }

            // Forces waiting for all the results and throws an exception if there was some failure
            CompletableFuture.allOf(allPublishResults.toArray(new CompletableFuture[0])).get();

        } catch (ExecutionException e) {
            String errorMsg = "Failed to publish all astral objects: " + e.getLocalizedMessage();
            System.err.println(errorMsg);
            throw new IOException(errorMsg, e.getCause());
        }
    }

    /**
     * Reads the goal for the current challenge from the Megaverse API.
     *
     * @return the goal Megaverse returned from the Megaverse API.
     * @throws InterruptedException if the thread was interrupted while reading the API.
     * @throws IOException          if there was an error reading the API.
     */
    public @NonNull Megaverse readGoal() throws InterruptedException, IOException {

        try (HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()) {

            HttpRequest request = HttpRequest.newBuilder()
                .uri(API_ROOT.resolve(String.format(GOAL_ENDPOINT_FORMAT, candidateId)))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IOException("Error HTTP response: " + response + " -> " + response.body());
            }

            ObjectMapper jsonMapper = new ObjectMapper();
            Map<String, String[][]> goalMap = jsonMapper.readValue(response.body(), new TypeReference<>() {
            });

            String[][] goalMatrix = goalMap.get("goal");

            SpaceCell[][] spaceCells = new SpaceCell[goalMatrix.length][];
            Megaverse megaverse = new Megaverse(candidateId, spaceCells);

            for (int i = 0; i < goalMatrix.length; ++i) {
                spaceCells[i] = new SpaceCell[goalMatrix[i].length];
                for (int j = 0; j < goalMatrix[i].length; ++j) {
                    spaceCells[i][j] = new SpaceCell(megaverse, i, j).fillFromString(goalMatrix[i][j]);
                }
            }

            return megaverse;
        }
    }
}
