package io.quarkus.resteasy.reactive.jackson.deployment.test.streams;

import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
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
import org.jboss.resteasy.reactive.server.jackson.JacksonBasicMessageBodyReader;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.smallrye.mutiny.Multi;

public class StreamTestCase {

    @TestHTTPResource
    URI uri;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(StreamResource.class, Message.class, Demands.class));

    @Test
    public void testSseFromSse() throws Exception {
        testSse("streams");
    }

    @Test
    public void testSseFromMulti() throws Exception {
        testSse("streams/multi");
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
        testMulti("streams");
    }

    @Test
    public void testMultiFromMulti() {
        testMulti("streams/multi");
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
        testJsonMulti("streams/json");
        testJsonMulti("streams/json2");
        testJsonMulti("streams/blocking/json");
    }

    @Test
    public void testJsonMultiFromMulti() {
        testJsonMulti("streams/json/multi");
        testJsonMulti("streams/json/multi-alt");
    }

    @Test
    public void testJsonMultiFromMultiWithDefaultElementType() {
        testJsonMulti("streams/json/multi2");
    }

    @Test
    public void testJsonMultiMultiDoc() {
        when().get(uri.toString() + "streams/json/multi-docs")
                .then().statusCode(HttpStatus.SC_OK)
                // @formatter:off
                .body(is("{\"name\":\"hello\"}\n"
                            + "{\"name\":\"stef\"}\n"))
                // @formatter:on
                .header(HttpHeaders.CONTENT_TYPE, containsString(RestMediaType.APPLICATION_JSON));
    }

    @Test
    public void testJsonMultiMultiDocHigherDemand() {
        when().get(uri.toString() + "streams/json/multi-docs-huge-demand")
                .then().statusCode(HttpStatus.SC_OK)
                // @formatter:off
                .body(allOf(
                    containsString("{\"name\":\"hello\"}\n"),
                    containsString("{\"name\":\"stef\"}\n"),
                    containsString("{\"name\":\"snazy\"}\n"),
                    containsString("{\"name\":\"elani\"}\n"),
                    containsString("{\"name\":\"foo\"}\n"),
                    containsString("{\"name\":\"bar\"}\n"),
                    containsString("{\"name\":\"baz\"}\n"),
                    endsWith("{\"demands\":[5,5]}\n")))
                // @formatter:on
                .header(HttpHeaders.CONTENT_TYPE, containsString(RestMediaType.APPLICATION_JSON))
                .header("foo", equalTo("bar"));
    }

    @Test
    public void testNdJsonMultiFromMulti() {
        when().get(uri.toString() + "streams/ndjson/multi")
                .then().statusCode(HttpStatus.SC_OK)
                // @formatter:off
                .body(is("{\"name\":\"hello\"}\n"
                            + "{\"name\":\"stef\"}\n"))
                // @formatter:on
                .header(HttpHeaders.CONTENT_TYPE, containsString(RestMediaType.APPLICATION_NDJSON));
    }

    @Test
    public void testNdJsonMultiFromMulti2() {
        when().get(uri.toString() + "streams/ndjson/multi2")
                .then().statusCode(222)
                // @formatter:off
                .body(is("{\"name\":\"hello\"}\n"
                        + "{\"name\":\"stef\"}\n"))
                // @formatter:on
                .header(HttpHeaders.CONTENT_TYPE, containsString(RestMediaType.APPLICATION_NDJSON))
                .header("foo", "bar");
    }

    @Test
    public void testRestMultiEmptyJson() {
        when().get(uri.toString() + "streams/restmulti/empty")
                .then().statusCode(222)
                .body(is("[]"))
                .header("foo", "bar");
    }

    @Test
    public void testStreamJsonMultiFromMulti() {
        when().get(uri.toString() + "streams/stream-json/multi")
                .then().statusCode(HttpStatus.SC_OK)
                // @formatter:off
                .body(is("{\"name\":\"hello\"}\n"
                        + "{\"name\":\"stef\"}\n"))
                // @formatter:on
                .header(HttpHeaders.CONTENT_TYPE, containsString(RestMediaType.APPLICATION_STREAM_JSON));
    }

    private void testJsonMulti(String path) {
        Client client = ClientBuilder.newBuilder().register(new JacksonBasicMessageBodyReader(new ObjectMapper())).build();
        WebTarget target = client.target(uri.toString() + path);
        Multi<Message> multi = target.request().rx(MultiInvoker.class).get(Message.class);
        List<Message> list = multi.collect().asList().await().atMost(Duration.ofSeconds(30));
        assertThat(list).extracting("name").containsExactly("hello", "stef");
    }

    /**
     * Reproduce <a href="https://github.com/quarkusio/quarkus/issues/30044">#30044</a>.
     */
    @Test
    public void testStreamJsonMultiFromMultiFast() {
        String payload = when().get(uri.toString() + "streams/stream-json/multi/fast")
                .then().statusCode(HttpStatus.SC_OK)
                .header(HttpHeaders.CONTENT_TYPE, containsString(RestMediaType.APPLICATION_STREAM_JSON))
                .extract().response().asString();

        // the payload include 5000 json objects
        assertThat(payload.lines()).hasSize(5000)
                .allSatisfy(s -> assertThat(s).matches("\\{\"name\":\".*\"}"));
    }
}
