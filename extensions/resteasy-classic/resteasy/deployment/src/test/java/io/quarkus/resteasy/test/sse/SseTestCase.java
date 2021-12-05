package io.quarkus.resteasy.test.sse;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.SseEventSource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class SseTestCase {

    @TestHTTPResource
    URI uri;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(SseResource.class));

    @Test
    public void testSse() throws Exception {

        Client client = ClientBuilder.newBuilder().build();
        WebTarget target = client.target(uri.toString() + "sse");
        try (SseEventSource eventSource = SseEventSource.target(target).build()) {
            CompletableFuture<String> res = new CompletableFuture<>();
            eventSource.register(new Consumer<InboundSseEvent>() {
                @Override
                public void accept(InboundSseEvent inboundSseEvent) {
                    res.complete(inboundSseEvent.readData());
                }
            }, new Consumer<Throwable>() {
                @Override
                public void accept(Throwable throwable) {
                    res.completeExceptionally(throwable);
                }
            });
            eventSource.open();
            Assertions.assertEquals("hello", res.get(5, TimeUnit.SECONDS));
        }
    }
}
