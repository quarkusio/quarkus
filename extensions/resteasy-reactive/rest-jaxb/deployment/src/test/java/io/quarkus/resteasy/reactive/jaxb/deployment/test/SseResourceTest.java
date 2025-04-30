package io.quarkus.resteasy.reactive.jaxb.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import jakarta.ws.rs.ConstrainedTo;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.sse.InboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseBroadcaster;
import jakarta.ws.rs.sse.SseEventSink;
import jakarta.ws.rs.sse.SseEventSource;
import jakarta.xml.bind.JAXB;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import org.jboss.resteasy.reactive.RestStreamElementType;
import org.jboss.resteasy.reactive.client.impl.MultiInvoker;
import org.jboss.resteasy.reactive.common.util.StreamUtil;
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
                    .addClasses(SseResource.class, Message.class, ClientJaxbMessageBodyReader.class));

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
        @RestStreamElementType(MediaType.APPLICATION_XML)
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
        @RestStreamElementType(MediaType.APPLICATION_XML)
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

    @ConstrainedTo(RuntimeType.CLIENT)
    @Provider
    public static class ClientJaxbMessageBodyReader implements MessageBodyReader<Object> {

        @Override
        public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
                throws WebApplicationException, IOException {
            return doReadFrom(type, genericType, entityStream);
        }

        @Override
        public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return isReadable(mediaType, type);
        }

        protected boolean isReadable(MediaType mediaType, Class<?> type) {
            if (mediaType == null) {
                return false;
            }
            if (String.class.equals(type)) { // don't attempt to read plain strings
                return false;
            }
            String subtype = mediaType.getSubtype();
            boolean isCorrectMediaType = "application".equals(mediaType.getType()) || "text".equals(mediaType.getType());
            return (isCorrectMediaType && "xml".equalsIgnoreCase(subtype) || subtype.endsWith("+xml"))
                    || (mediaType.isWildcardSubtype() && (mediaType.isWildcardType() || isCorrectMediaType));
        }

        private Object doReadFrom(Class<Object> type, Type genericType, InputStream entityStream) throws IOException {
            if (isInputStreamEmpty(entityStream)) {
                return null;
            }

            return JAXB.unmarshal(entityStream, type);
        }

        private boolean isInputStreamEmpty(InputStream entityStream) throws IOException {
            return StreamUtil.isEmpty(entityStream) || entityStream.available() == 0;
        }
    }
}
