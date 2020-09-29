package io.quarkus.rest.runtime.providers.serialisers.jsonb;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;

@Provider
@Consumes({ "application/json", "application/*+json", "text/json" })
public class JsonbMessageBodyReader implements MessageBodyReader<Object> {

    private final Jsonb json;

    public JsonbMessageBodyReader() {
        ArcContainer arcContainer = Arc.container();
        if (arcContainer != null) {
            InstanceHandle<Jsonb> jsonbInstanceHandle = Arc.container().instance(Jsonb.class);
            if (jsonbInstanceHandle.isAvailable()) {
                this.json = jsonbInstanceHandle.get();
            } else {
                this.json = JsonbBuilder.create();
            }
        } else {
            this.json = JsonbBuilder.create();
        }
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    @Override
    public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        Type runtimeType = genericType != null ? genericType : type;
        return json.fromJson(entityStream, runtimeType);
    }
}
