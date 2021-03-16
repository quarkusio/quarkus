package org.jboss.resteasy.reactive.common.providers.serialisers.jsonp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.json.JsonObject;
import javax.json.JsonWriter;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

public class JsonObjectHandler implements MessageBodyReader<JsonObject>, MessageBodyWriter<JsonObject> {
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return JsonObject.class.isAssignableFrom(type);
    }

    public void writeTo(JsonObject o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        try (JsonWriter writer = JsonpUtil.writer(entityStream, mediaType)) {
            writer.writeObject(o);
        }
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return JsonObject.class.isAssignableFrom(type);
    }

    @Override
    public JsonObject readFrom(Class<JsonObject> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        return JsonpUtil.reader(entityStream, mediaType).readObject();
    }
}
