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
 * The {@code MegaverseConnection} class provides methods for interacting with a remote API
 * that manages the state and structure of a Megaverse. The class facilitates operations such as
 * publishing astral objects within the Megaverse or retrieving goal matrices to construct a Megaverse representation.
 * <p>
 * The class enforces retry mechanisms for API interactions to handle rate-limiting scenarios, providing a robust
 * mechanism for communication with the remote endpoints.
 */
public class MegaverseConnection {

    public static final URI API_ROOT = URI.create("https://challenge.crossmint.io/api/");
    public static final String GOAL_ENDPOINT_FORMAT = "map/%s/goal";

    public static final int MAX_RETRIES = 15;
    public static final int MIN_RETRY_DELAY_MS = 8000;

    private static void tooManyRequestsHandler(HttpResponse<String> response, Throwable error) {
        if (response.statusCode() == 429) {
            String errorMsg = "Too many requests: " + response + " -> " + response.body();
            System.err.println(errorMsg);
            throw new UncheckedIOException(new IOException(errorMsg));
        } else if (error == null && response.statusCode() / 100 == 2) {
            System.out.println("Success: " + response);
        }
    }

    @NonNull
    private final String candidateId;

    public MegaverseConnection(@NonNull String candidateId) {
        this.candidateId = candidateId;
    }

    /**
     * Attempts to send an HTTP request using an asynchronous HTTP client with retry logic.
     * The method will retry the request up to a maximum number of retries if an exception occurs or
     * a failure response is received. Random delays are introduced between retries.
     * Each retry respects a minimum delay interval.
     *
     * @param httpClient the instance of {@link HttpClient} used to send the HTTP request asynchronously.
     * @param request    the {@link HttpRequest} to be sent to the server.
     * @return a {@link CompletableFuture} representing the eventual completion of the HTTP request,
     * which contains the {@link HttpResponse} or a failure.
     */
    private CompletableFuture<HttpResponse<String>> sendWithRetries(HttpClient httpClient, HttpRequest request) {

        CompletableFuture<HttpResponse<String>> futureResponse = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .whenCompleteAsync(MegaverseConnection::tooManyRequestsHandler);

        for (int i = 0; i < MAX_RETRIES; ++i) {
            futureResponse = futureResponse.exceptionallyComposeAsync(
                _ -> {
                    System.out.println("Retrying... " + request);
                    return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .whenCompleteAsync(MegaverseConnection::tooManyRequestsHandler);
                },
                //Random delay for each retry
                CompletableFuture.delayedExecutor(MIN_RETRY_DELAY_MS + Math.round(MIN_RETRY_DELAY_MS * Math.random()), TimeUnit.MILLISECONDS));
        }

        return futureResponse;
    }

    /**
     * Publishes the provided astral object by sending an HTTP POST request with the object's data serialized as JSON.
     * This method constructs the HTTP request using the astral object's endpoint and invokes the retry logic for
     * sending the request asynchronously.
     *
     * @param httpClient   the {@link HttpClient} instance used to send the HTTP request asynchronously.
     * @param astralObject the {@link AstralObject} containing the data to be published and its destination endpoint.
     * @return a {@link CompletableFuture} that represents the eventual completion of the HTTP request,
     * containing the {@link HttpResponse} with the server's response or an exceptional state if the request fails.
     * @throws IllegalArgumentException if the astral object cannot be serialized to JSON.
     */
    private CompletableFuture<HttpResponse<String>> publishAstralObject(HttpClient httpClient, AstralObject astralObject) {

        String jsonBody;
        try {
            ObjectMapper jsonMapper = new ObjectMapper();
            jsonBody = jsonMapper.writeValueAsString(astralObject);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Could not parse astralObject: " + astralObject);
        }

        HttpRequest request = HttpRequest.newBuilder()
            .uri(API_ROOT.resolve(astralObject.endpoint()))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();

        return sendWithRetries(httpClient, request);
    }

    /**
     * Publishes the state of all astral objects within a given Megaverse. This method iterates
     * over each {@link SpaceCell} in the Megaverse, identifies cells with astral objects, and
     * attempts to publish the objects to their respective endpoints using the helper method
     * {@code publishWithRetry}.
     *
     * @param megaverse the {@link Megaverse} instance containing the space cells and astral objects to be published.
     * @throws InterruptedException if interrupted while waiting for the publishing process to complete.
     * @throws IOException          if a failure occurs during the publishing process or if the HTTP client fails.
     */
    public void publishState(@NonNull Megaverse megaverse) throws InterruptedException, IOException {

        LinkedList<CompletableFuture<HttpResponse<String>>> allPublishResults = new LinkedList<>();

        try (HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()) {

            for (SpaceCell[] row : megaverse.spaceCells()) {
                for (SpaceCell cell : row) {
                    cell.getAstralObject().ifPresent(
                        astralObject -> allPublishResults.add(publishAstralObject(httpClient, astralObject)));
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
     * Retrieves and parses a goal matrix from a remote API, converting it into a {@link Megaverse} instance.
     * This method sends an HTTP GET request to a specified endpoint, processes the JSON response, and
     * constructs a {@link Megaverse} populated with {@link SpaceCell} objects.
     *
     * @return a {@link Megaverse} instance built from the retrieved goal matrix.
     * @throws InterruptedException if the thread is interrupted during the HTTP request or response processing.
     * @throws IOException          if an I/O error occurs during the HTTP request or while processing the response.
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
