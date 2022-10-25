package io.quarkus.resteasy.reactive.jackson.deployment.test.sse;

import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.sse.InboundSseEvent;
import jakarta.ws.rs.sse.SseEventSource;

import org.apache.http.HttpStatus;
import org.jboss.resteasy.reactive.client.impl.MultiInvoker;
import org.jboss.resteasy.reactive.common.util.RestMediaType;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
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
            List<String> collect = Collections.synchronizedList(new ArrayList<>());
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
            assertThat(res.get(5, TimeUnit.SECONDS)).containsExactly("hello", "stef");
        }
    }

    @Test
    public void testMultiFromSse() {
        testMulti("sse");
    }

    @Test
    public void testMultiFromMulti() {
        testMulti("sse/multi");
    }

    private void testMulti(String path) {
        Client client = ClientBuilder.newBuilder().build();
        WebTarget target = client.target(uri.toString() + path);
        Multi<String> multi = target.request().rx(MultiInvoker.class).get(String.class);
        List<String> list = multi.collect().asList().await().atMost(Duration.ofSeconds(30));
        assertThat(list).containsExactly("hello", "stef");
    }

    @Test
    public void testJsonMultiFromSse() {
        testJsonMulti("sse/json");
        testJsonMulti("sse/json2");
        testJsonMulti("sse/blocking/json");
    }

    @Test
    public void testJsonMultiFromMulti() {
        testJsonMulti("sse/json/multi");
    }

    @Test
    public void testJsonMultiFromMultiWithDefaultElementType() {
        testJsonMulti("sse/json/multi2");
    }

    @Test
    public void testNdJsonMultiFromMulti() {
        when().get(uri.toString() + "sse/ndjson/multi")
                .then().statusCode(HttpStatus.SC_OK)
                // @formatter:off
                .body(is("{\"name\":\"hello\"}\n"
                            + "{\"name\":\"stef\"}\n"))
                // @formatter:on
                .header(HttpHeaders.CONTENT_TYPE, containsString(RestMediaType.APPLICATION_NDJSON));
    }

    @Test
    public void testStreamJsonMultiFromMulti() {
        when().get(uri.toString() + "sse/stream-json/multi")
                .then().statusCode(HttpStatus.SC_OK)
                // @formatter:off
                .body(is("{\"name\":\"hello\"}\n"
                        + "{\"name\":\"stef\"}\n"))
                // @formatter:on
                .header(HttpHeaders.CONTENT_TYPE, containsString(RestMediaType.APPLICATION_STREAM_JSON));
    }

    private void testJsonMulti(String path) {
        Client client = ClientBuilder.newBuilder().build();
        WebTarget target = client.target(uri.toString() + path);
        Multi<Message> multi = target.request().rx(MultiInvoker.class).get(Message.class);
        List<Message> list = multi.collect().asList().await().atMost(Duration.ofSeconds(30));
        assertThat(list).extracting("name").containsExactly("hello", "stef");
    }
}
