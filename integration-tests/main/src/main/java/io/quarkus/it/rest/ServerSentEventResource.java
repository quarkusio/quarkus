package io.quarkus.it.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;

import org.jboss.resteasy.annotations.SseElementType;

@Path("/sse")
public class ServerSentEventResource {

    private Sse sse;

    @Context
    public void setSse(final Sse sse) {
        this.sse = sse;
    }

    @GET
    @Path("/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void sendData(@Context SseEventSink sink) {
        // send a stream of few events
        try {
            for (int i = 0; i < 3; i++) {
                final OutboundSseEvent.Builder builder = this.sse.newEventBuilder();
                builder.id(String.valueOf(i)).mediaType(MediaType.TEXT_PLAIN_TYPE)
                        .data(Integer.class, i)
                        .name("stream of numbers");

                sink.send(builder.build());
            }
        } finally {
            sink.close();
        }
    }

    @GET
    @Path("/stream-html")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @SseElementType("text/html")
    public void sendHtmlData(@Context SseEventSink sink) {
        // send a stream of few events
        try {
            for (int i = 0; i < 3; i++) {
                final OutboundSseEvent.Builder builder = this.sse.newEventBuilder();
                builder.id(String.valueOf(i)).mediaType(MediaType.TEXT_HTML_TYPE)
                        .data("<html><body>" + i + "</body></html>")
                        .name("stream of pages");

                sink.send(builder.build());
            }
        } finally {
            sink.close();
        }
    }

    @GET
    @Path("/stream-xml")
    @SseElementType(MediaType.TEXT_XML)
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void sendXmlData(@Context SseEventSink sink) {
        // send a stream of few events
        try {
            for (int i = 0; i < 3; i++) {
                final OutboundSseEvent.Builder builder = this.sse.newEventBuilder();
                builder.id(String.valueOf(i)).mediaType(MediaType.TEXT_XML_TYPE)
                        .data("<settings><foo bar=\"" + i + "\"/></settings>")
                        .name("stream of XML data");

                sink.send(builder.build());
            }
        } finally {
            sink.close();
        }
    }
}
