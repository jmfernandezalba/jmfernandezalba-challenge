package com.crossmint.challenge.connectors;

import com.crossmint.challenge.model.AstralObject;
import com.crossmint.challenge.model.Megaverse;
import com.crossmint.challenge.model.SpaceCell;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MegaverseConnectionTest {

    @Spy
    private MegaverseConnection connection;
    @Mock
    private HttpClient mockHttpClient;
    @Mock
    private HttpResponse<String> mockHttpResponse;

    private Megaverse buildDummyMegaverse() {
        SpaceCell[][] grid = new SpaceCell[2][2];
        Megaverse dummyMegaverse = new Megaverse("testCandidateId", grid);
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                grid[i][j] = new SpaceCell(dummyMegaverse, i, j);
            }
        }
        grid[0][1].fillFromString("POLYANET");
        return dummyMegaverse;
    }

    @BeforeEach
    public void setUp() {
        when(connection.buildHttpClient())
            .thenReturn(mockHttpClient);
    }

    @Test
    public void testPublishStatePostsCorrectly() throws IOException {
        // Arrange: Define the input parameters and mocks.
        Megaverse dummyMegaverse = buildDummyMegaverse();
        AstralObject dummyObject = dummyMegaverse.spaceCells()[0][1].getAstralObject().orElse(null);
        HttpRequest dummyRequest = connection.buildPOSTRequest(dummyObject);

        when(mockHttpResponse.request())
            .thenReturn(dummyRequest);
        // - Succeed at first try
        when(mockHttpResponse.statusCode())
            .thenReturn(200);
        when(mockHttpClient.sendAsync(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
            .thenReturn(CompletableFuture.completedFuture(mockHttpResponse));

        // Act: publish the state
        connection.publishState(dummyMegaverse);

        // Assert: check the client is called once
        verify(mockHttpClient, times(1))
            .sendAsync(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()));
    }

    @Test
    public void testPublishStateDoesNotRetryOn500Error() {
        // Arrange: Define the input parameters and mocks.
        Megaverse dummyMegaverse = buildDummyMegaverse();

        // - Fail with a 500
        when(mockHttpResponse.statusCode())
            .thenReturn(500);
        when(mockHttpClient.sendAsync(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
            .thenReturn(CompletableFuture.completedFuture(mockHttpResponse));

        // Act & Assert: check the 500 error is not retried and method throws exception
        assertThrows(IOException.class, () -> connection.publishState(dummyMegaverse));

        verify(mockHttpClient, times(1))
            .sendAsync(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()));
    }

    @Test
    public void testPublishStateRetriesOnError() throws IOException {
        // Arrange: Define the input parameters and mocks.
        Megaverse dummyMegaverse = buildDummyMegaverse();
        AstralObject dummyObject = dummyMegaverse.spaceCells()[0][1].getAstralObject().orElse(null);
        HttpRequest dummyRequest = connection.buildPOSTRequest(dummyObject);

        when(mockHttpResponse.request())
            .thenReturn(dummyRequest);
        // - Fail the first attempt and succeed the second
        when(mockHttpResponse.statusCode())
            .thenReturn(200);
        when(mockHttpClient.sendAsync(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
            .thenReturn(CompletableFuture.failedFuture(new IOException("Network error")))
            .thenReturn(CompletableFuture.completedFuture(mockHttpResponse));

        // Act: publish the state
        connection.publishState(dummyMegaverse);

        // Assert: check the client is called twice
        verify(mockHttpClient, times(2))
            .sendAsync(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()));
    }

    @Test
    public void testPublishStateRetriesOn429Error() throws IOException {
        // Arrange: Define the input parameters and mocks.
        Megaverse dummyMegaverse = buildDummyMegaverse();
        AstralObject dummyObject = dummyMegaverse.spaceCells()[0][1].getAstralObject().orElse(null);
        HttpRequest dummyRequest = connection.buildPOSTRequest(dummyObject);

        when(mockHttpResponse.request())
            .thenReturn(dummyRequest);
        // - Fail the first attempt and succeed the second
        when(mockHttpResponse.statusCode())
            .thenReturn(429)
            .thenReturn(200);

        when(mockHttpClient.sendAsync(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
            .thenReturn(CompletableFuture.completedFuture(mockHttpResponse));

        // Act: publish the state
        connection.publishState(dummyMegaverse);

        // Assert: check the client is called twice
        verify(mockHttpClient, times(2))
            .sendAsync(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()));
    }
}
