package io.quarkus.rest.runtime.providers.serialisers.jsonb;

import java.io.ByteArrayOutputStream;
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
import javax.ws.rs.ext.Provider;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.rest.runtime.core.LazyMethod;
import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.runtime.spi.QuarkusRestMessageBodyWriter;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;

@Provider
@Produces({ "application/json", "application/*+json", "text/json" })
public class JsonbMessageBodyWriter implements QuarkusRestMessageBodyWriter<Object> {

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
        return !String.class.equals(type);
    }

    @Override
    public void writeTo(Object o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        json.toJson(o, type, entityStream);
    }

    @Override
    public boolean isWriteable(Class<?> type, LazyMethod target, MediaType mediaType) {
        return !String.class.equals(type);
    }

    @Override
    public void writeResponse(Object o, QuarkusRestRequestContext context) throws WebApplicationException {
        HttpServerResponse vertxResponse = context.getContext().response();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        json.toJson(o, baos);
        vertxResponse.end(Buffer.buffer(baos.toByteArray()));
    }
}
