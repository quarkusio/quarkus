package io.quarkus.resteasy.reactive.jaxb.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseBroadcaster;
import javax.ws.rs.sse.SseEventSink;
import javax.ws.rs.sse.SseEventSource;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.jboss.resteasy.reactive.RestSseElementType;
import org.jboss.resteasy.reactive.client.impl.MultiInvoker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Multi;

public class SseResourceTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(SseResource.class, Message.class));

    @TestHTTPResource
    URI uri;

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
            assertThat(res.get(5, TimeUnit.SECONDS)).containsExactly("hello");
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
        assertThat(list).containsExactly("hello");
    }

    @Test
    public void testXmlMultiFromSse() {
        testXmlMulti("sse/xml");
        testXmlMulti("sse/xml2");
        testXmlMulti("sse/blocking/xml");
    }

    private void testXmlMulti(String path) {
        Client client = ClientBuilder.newBuilder().build();
        WebTarget target = client.target(uri.toString() + path);
        Multi<Message> multi = target.request().rx(MultiInvoker.class).get(Message.class);
        List<Message> list = multi.collect().asList().await().atMost(Duration.ofSeconds(30));
        assertThat(list).extracting("name").containsExactly("hello");
    }

    @XmlRootElement
    public static class Message {
        @XmlElement
        public String name;

        public Message(String name) {
            this.name = name;
        }

        // for JAXB
        public Message() {
        }
    }

    @Path("sse")
    public static class SseResource {

        @GET
        @Produces(MediaType.SERVER_SENT_EVENTS)
        public void sse(Sse sse, SseEventSink sink) {
            if (sink == null) {
                throw new IllegalStateException("No client connected.");
            }
            SseBroadcaster sseBroadcaster = sse.newBroadcaster();

            sseBroadcaster.register(sink);
            sseBroadcaster.broadcast(sse.newEventBuilder().data("hello").build()).thenAccept(v -> sseBroadcaster.close());
        }

        @Path("multi")
        @GET
        @Produces(MediaType.SERVER_SENT_EVENTS)
        public Multi<String> multiText() {
            return Multi.createFrom().items("hello");
        }

        @Path("xml")
        @GET
        @Produces(MediaType.SERVER_SENT_EVENTS)
        @RestSseElementType(MediaType.APPLICATION_XML)
        public void sseXml(Sse sse, SseEventSink sink) {
            if (sink == null) {
                throw new IllegalStateException("No client connected.");
            }
            SseBroadcaster sseBroadcaster = sse.newBroadcaster();

            sseBroadcaster.register(sink);
            sseBroadcaster.broadcast(sse.newEventBuilder().data(new Message("hello")).build())
                    .thenAccept(v -> sseBroadcaster.close());
        }

        @Blocking
        @Path("blocking/xml")
        @GET
        @Produces(MediaType.SERVER_SENT_EVENTS)
        @RestSseElementType(MediaType.APPLICATION_XML)
        public void blockingSseXml(Sse sse, SseEventSink sink) {
            if (sink == null) {
                throw new IllegalStateException("No client connected.");
            }
            SseBroadcaster sseBroadcaster = sse.newBroadcaster();

            sseBroadcaster.register(sink);
            sseBroadcaster.broadcast(sse.newEventBuilder().data(new Message("hello")).build())
                    .thenAccept(v -> sseBroadcaster.close());
        }

        @Path("xml2")
        @GET
        @Produces(MediaType.SERVER_SENT_EVENTS)
        public void sseXml2(Sse sse, SseEventSink sink) {
            if (sink == null) {
                throw new IllegalStateException("No client connected.");
            }
            SseBroadcaster sseBroadcaster = sse.newBroadcaster();

            // Same as sseXml but set mediaType in builder
            sseBroadcaster.register(sink);
            sseBroadcaster
                    .broadcast(
                            sse.newEventBuilder().data(new Message("hello")).mediaType(MediaType.APPLICATION_XML_TYPE).build())
                    .thenAccept(v -> sseBroadcaster.close());
        }

    }
}
