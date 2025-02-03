package com.crossmint.challenge.connectors;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Flow;

/**
 * Utility class for handling logging-related functionality.
 * <p>
 * This class provides methods and inner implementations for processing
 * and handling HTTP request bodies, allowing extraction and further manipulation of
 * their content. It also contains a private implementation of a subscriber to handle
 * data flow during HTTP communications.
 */
public class LoggingUtils {

    /**
     * Extracts the body content from an HTTP request.
     *
     * @param httpRequest the HTTP request from which the body is to be extracted
     * @return the body of the HTTP request as a string
     */
    public static String extractBodyFromRequest(HttpRequest httpRequest) {
        if (httpRequest.method().equals("POST")) {
            return httpRequest.bodyPublisher().map(p -> {
                var bodySubscriber = HttpResponse.BodySubscribers.ofString(StandardCharsets.UTF_8);
                var flowSubscriber = new LoggingUtils.StringSubscriber(bodySubscriber);
                p.subscribe(flowSubscriber);
                return bodySubscriber.getBody().toCompletableFuture().join();
            }).orElseThrow();
        } else {
            return "";
        }
    }

    private record StringSubscriber(HttpResponse.BodySubscriber<String> wrapped) implements Flow.Subscriber<ByteBuffer> {

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            wrapped.onSubscribe(subscription);
        }

        @Override
        public void onNext(ByteBuffer item) {
            wrapped.onNext(List.of(item));
        }

        @Override
        public void onError(Throwable throwable) {
            wrapped.onError(throwable);
        }

        @Override
        public void onComplete() {
            wrapped.onComplete();
        }
    }
}
