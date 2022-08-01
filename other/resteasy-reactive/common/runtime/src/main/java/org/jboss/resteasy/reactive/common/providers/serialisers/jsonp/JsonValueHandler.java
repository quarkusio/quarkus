package org.jboss.resteasy.reactive.common.providers.serialisers.jsonp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.json.JsonValue;
import javax.json.JsonWriter;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

public class JsonValueHandler implements MessageBodyReader<JsonValue>, MessageBodyWriter<JsonValue> {
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return JsonValue.class.isAssignableFrom(type);
    }

    public void writeTo(JsonValue jsonValue, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        try (JsonWriter writer = JsonpUtil.writer(entityStream, mediaType)) {
            writer.write(jsonValue);
        }
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return JsonValue.class.isAssignableFrom(type);
    }

    @Override
    public JsonValue readFrom(Class<JsonValue> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        return JsonpUtil.reader(entityStream, mediaType).readValue();
    }
}
