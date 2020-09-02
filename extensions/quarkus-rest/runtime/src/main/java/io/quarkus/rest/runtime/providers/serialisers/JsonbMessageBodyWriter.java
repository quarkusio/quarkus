package io.quarkus.rest.runtime.providers.serialisers;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;

// NOTE: currently disabled to benchmark the vertx one
@Provider
@Produces({ "application/json", "application/*+json", "text/json" })
public class JsonbMessageBodyWriter implements MessageBodyWriter<Object> {

    private final Jsonb json;

    public JsonbMessageBodyWriter() {
        InstanceHandle<Jsonb> jsonbInstanceHandle = Arc.container().instance(Jsonb.class);
        if (jsonbInstanceHandle.isAvailable()) {
            this.json = jsonbInstanceHandle.get();
        } else {
            this.json = JsonbBuilder.create();
        }
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    @Override
    public void writeTo(Object o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        json.toJson(o, type, entityStream);
    }
}
