package com.crossmint.challenge.connectors;

import com.crossmint.challenge.model.AstralObject;
import com.crossmint.challenge.model.Megaverse;
import com.crossmint.challenge.model.SpaceCell;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.Setter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
@Setter
public class MegaverseConnection {

    public static final URI API_ROOT = URI.create("https://challenge.crossmint.io/api/");
    public static final String GOAL_ENDPOINT_FORMAT = "map/%s/goal";

    public static final int INITIAL_DELAY_MS = 900;
    public static final int MAX_RETRIES = 10;
    public static final int MIN_RETRY_DELAY_MS = 8000;
    @NonNull
    private String candidateId;

    // This is being used by the tests mocks
    @SuppressWarnings("unused")
    public MegaverseConnection() {
        this.candidateId = "";
    }

    public MegaverseConnection(@NonNull String candidateId) {
        this.candidateId = candidateId;
    }

    /**
     * Processes the 429 response to throw an exception so we can retry. Other non 2xx return messages (like 5xx) do not throw
     * exceptions and hence are not retried.
     *
     * @param response the response from the server.
     * @param error    the error thrown by the http client.
     */
    private static void tooManyRequestsHandler(HttpResponse<String> response, Throwable error) {
        if (error == null) {
            int statusCode = response.statusCode();
            if (statusCode / 100 == 2) {
                System.out.println("SUCCESS " + response + " <- " + LoggingUtils.extractBodyFromRequest(response.request()));
            } else if (statusCode == 429) {
                String errorMsg = "FAILURE " + response + " <- " + LoggingUtils.extractBodyFromRequest(response.request());
                System.err.println(errorMsg);
                throw new UncheckedIOException(new IOException(errorMsg));
            }
        }
    }


    HttpClient buildHttpClient() {
        return HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    }

    HttpRequest buildGoalRequest() {
        return HttpRequest.newBuilder()
            .uri(API_ROOT.resolve(String.format(GOAL_ENDPOINT_FORMAT, candidateId)))
            .build();
    }

    HttpRequest buildPOSTRequest(AstralObject astralObject) {

        String jsonBody;
        try {
            ObjectMapper jsonMapper = new ObjectMapper();
            jsonBody = jsonMapper.writeValueAsString(astralObject);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Could not parse astralObject: " + astralObject);
        }

        return HttpRequest.newBuilder()
            .uri(API_ROOT.resolve(astralObject.endpoint()))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();
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
    CompletableFuture<HttpResponse<String>> sendWithRetries(HttpClient httpClient, HttpRequest request) throws InterruptedException {

        Thread.sleep(INITIAL_DELAY_MS);

        CompletableFuture<HttpResponse<String>> futureResponse = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .whenCompleteAsync(MegaverseConnection::tooManyRequestsHandler);

        double exponentialDelay = MIN_RETRY_DELAY_MS;
        for (int i = 0; i < MAX_RETRIES; ++i) {
            final int tryNumber = i + 1;
            futureResponse = futureResponse.exceptionallyComposeAsync(
                error -> {
                    System.out.println("RETRY (" + tryNumber + "/" + MAX_RETRIES + ") " + request + " <- " + LoggingUtils.extractBodyFromRequest(request));
                    return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .whenCompleteAsync(MegaverseConnection::tooManyRequestsHandler);
                },
                //Random delay for each retry
                CompletableFuture.delayedExecutor(Math.round(MIN_RETRY_DELAY_MS + exponentialDelay * Math.random()), TimeUnit.MILLISECONDS));
            exponentialDelay *= 1.2;
        }

        return futureResponse;
    }

    Megaverse buildMegaverse(String[][] goalMatrix) {

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

    /**
     * Publishes the state of all astral objects within a given Megaverse. This method iterates
     * over each {@link SpaceCell} in the Megaverse, identifies cells with astral objects, and
     * attempts to publish the objects to their respective endpoints using the helper method
     * {@code sendWithRetries}.
     *
     * @param megaverse the {@link Megaverse} instance containing the space cells and astral objects to be published.
     * @throws IOException          if a failure occurs during the publishing process or if the HTTP client fails.
     */
    public void publishState(@NonNull Megaverse megaverse) throws IOException, InterruptedException {

        try (HttpClient httpClient = buildHttpClient()) {

            LinkedList<CompletableFuture<HttpResponse<String>>> allPublishResults = new LinkedList<>();

            for (SpaceCell[] row : megaverse.spaceCells()) {
                for (SpaceCell cell : row) {
                    Optional<AstralObject> optAstralObject = cell.getAstralObject();
                    if (optAstralObject.isPresent()) {
                        allPublishResults.add(sendWithRetries(httpClient, buildPOSTRequest(optAstralObject.get())));
                    }
                }
            }

            // Wait for all results to complete and throw exception if some completed exceptionally.
            CompletableFuture.allOf(allPublishResults.toArray(CompletableFuture[]::new)).get();

            // Check all results that didn't return 2xx
            List<HttpResponse<String>> failedResponses = allPublishResults.stream()
                .map(CompletableFuture::join)
                .filter(result -> result.statusCode() / 100 != 2)
                .toList();

            if (!failedResponses.isEmpty()) {
                throw new IOException("Failed to publish all astral objects: " + failedResponses);
            }
        } catch (ExecutionException e) {
            throw new IOException("Failed to publish all astral objects due to errors.", e.getCause());
        }
    }

    /**
     * Retrieves and parses a goal matrix from a remote API, converting it into a {@link Megaverse} instance.
     * This method sends an HTTP GET request to a specified endpoint, processes the JSON response, and
     * constructs a {@link Megaverse} populated with {@link SpaceCell} objects.
     *
     * @return a {@link Megaverse} instance built from the retrieved goal matrix.
     * @throws IOException          if an I/O error occurs during the HTTP request or while processing the response.
     */
    public @NonNull Megaverse readGoal() throws IOException, InterruptedException {

        try (HttpClient httpClient = buildHttpClient()) {

            HttpRequest request = buildGoalRequest();

            HttpResponse<String> response = sendWithRetries(httpClient, request).join();
            if (response.statusCode() / 100 != 2) {
                throw new IOException("Error HTTP response: " + response + " -> " + response.body());
            }

            ObjectMapper jsonMapper = new ObjectMapper();
            Map<String, String[][]> goalMap = jsonMapper.readValue(response.body(), new TypeReference<>() {
            });

            return buildMegaverse(goalMap.get("goal"));
        }
    }
}
