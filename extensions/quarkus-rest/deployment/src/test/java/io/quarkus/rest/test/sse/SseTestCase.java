package io.quarkus.rest.test.sse;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.SseEventSource;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestMultiInvoker;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.smallrye.mutiny.Multi;

public class SseTestCase {

    @TestHTTPResource
    URI uri;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(SseResource.class));

    @Test
    public void testSse() throws Exception {

        Client client = ClientBuilder.newBuilder().build();
        WebTarget target = client.target(uri.toString() + "sse");
        try (SseEventSource eventSource = SseEventSource.target(target).build()) {
            CompletableFuture<List<String>> res = new CompletableFuture<>();
            List<String> collect = new ArrayList<>();
            eventSource.register(new Consumer<InboundSseEvent>() {
                @Override
                public void accept(InboundSseEvent inboundSseEvent) {
                    collect.add(inboundSseEvent.readData());
                }
            }, new Consumer<Throwable>() {
                @Override
                public void accept(Throwable throwable) {
                    res.completeExceptionally(throwable);
                }
            }, () -> {
                res.complete(collect);
            });
            eventSource.open();
            Assertions.assertEquals(Arrays.asList("hello", "stef"), res.get(5, TimeUnit.SECONDS));
        }
    }

    @Test
    public void testSseMulti() throws Exception {
        Client client = ClientBuilder.newBuilder().build();
        WebTarget target = client.target(uri.toString() + "sse/multi");
        Multi<String> multi = target.request().rx(QuarkusRestMultiInvoker.class).get(String.class);
        List<String> list = multi.collectItems().asList().await().atMost(Duration.ofSeconds(5));
        Assertions.assertEquals(2, list.size());
        Assertions.assertEquals("hello", list.get(0));
        Assertions.assertEquals("stef", list.get(1));
    }
}
