package io.quarkus.it.rest;

import java.beans.ConstructorProperties;

import javax.json.bind.JsonbBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseEventSink;

import org.jboss.resteasy.annotations.SseElementType;

@Path("/constructorproperties")
public class TestResourceForConstructorProperties {

    @Context
    Sse sse;

    @GET
    @Path("/direct")
    @Produces(MediaType.APPLICATION_JSON)
    public VanillaJavaImmutableData direct() {
        return new VanillaJavaImmutableData("direct", "directvalue");
    }

    @GET
    @Path("/jsonb")
    @Produces(MediaType.APPLICATION_JSON)
    public String jsonb() {
        VanillaJavaImmutableData entity = new VanillaJavaImmutableData("jsonb", "jsonbvalue");
        return JsonbBuilder.create().toJson(entity);
    }

    @GET
    @Path("/response")
    @Produces(MediaType.APPLICATION_JSON)
    public Response response() {
        VanillaJavaImmutableData entity = new VanillaJavaImmutableData("response", "responsevalue");
        return Response.ok(entity).build();
    }

    @GET
    @Path("/sse")
    @SseElementType(MediaType.APPLICATION_JSON)
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void serverSentEvents(@Context SseEventSink sink) {
        VanillaJavaImmutableData data = new VanillaJavaImmutableData("sse", "ssevalue");
        try {
            OutboundSseEvent.Builder builder = sse.newEventBuilder();
            builder.id(String.valueOf(1))
                    .mediaType(MediaType.APPLICATION_JSON_TYPE)
                    .data(data)
                    .name("stream of json data");

            sink.send(builder.build());
        } finally {
            sink.close();
        }
    }

    public static class VanillaJavaImmutableData {
        private final String name;
        private final String value;

        @ConstructorProperties({ "name", "value" })
        public VanillaJavaImmutableData(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return "ConstructorPropertiesAnnotatedImmutableData [name=" + name + ", value=" + value + "]";
        }
    }
}
