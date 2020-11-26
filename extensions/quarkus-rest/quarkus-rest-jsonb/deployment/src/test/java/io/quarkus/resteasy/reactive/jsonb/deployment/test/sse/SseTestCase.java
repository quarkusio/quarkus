package io.quarkus.resteasy.reactive.jsonb.deployment.test.sse;

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

import org.jboss.resteasy.reactive.client.QuarkusRestMultiInvoker;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.smallrye.mutiny.Multi;

public class SseTestCase {

    @TestHTTPResource
    URI uri;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(SseResource.class, Message.class));

    @Test
    public void testSseFromSse() throws Exception {
        testSse("sse");
    }

    @Test
    public void testSseFromMulti() throws Exception {
        testSse("sse/multi");
    }

    private void testSse(String path) throws Exception {
        Client client = ClientBuilder.newBuilder().build();
        WebTarget target = client.target(uri.toString() + path);
        // do not reconnect
        try (SseEventSource eventSource = SseEventSource.target(target).reconnectingEvery(Integer.MAX_VALUE, TimeUnit.SECONDS)
                .build()) {
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
    public void testMultiFromSse() throws Exception {
        testMulti("sse");
    }

    @Test
    public void testMultiFromMulti() throws Exception {
        testMulti("sse/multi");
    }

    private void testMulti(String path) {
        Client client = ClientBuilder.newBuilder().build();
        WebTarget target = client.target(uri.toString() + path);
        Multi<String> multi = target.request().rx(QuarkusRestMultiInvoker.class).get(String.class);
        List<String> list = multi.collectItems().asList().await().atMost(Duration.ofSeconds(30));
        Assertions.assertEquals(2, list.size());
        Assertions.assertEquals("hello", list.get(0));
        Assertions.assertEquals("stef", list.get(1));
    }

    @Test
    public void testJsonMultiFromSse() throws Exception {
        testJsonMulti("sse/json");
        testJsonMulti("sse/json2");
    }

    @Test
    public void testJsonMultiFromMulti() throws Exception {
        testJsonMulti("sse/json/multi");
    }

    private void testJsonMulti(String path) {
        Client client = ClientBuilder.newBuilder().build();
        WebTarget target = client.target(uri.toString() + path);
        Multi<Message> multi = target.request().rx(QuarkusRestMultiInvoker.class).get(Message.class);
        List<Message> list = multi.collectItems().asList().await().atMost(Duration.ofSeconds(30));
        Assertions.assertEquals(2, list.size());
        Assertions.assertEquals("hello", list.get(0).name);
        Assertions.assertEquals("stef", list.get(1).name);
    }
}
