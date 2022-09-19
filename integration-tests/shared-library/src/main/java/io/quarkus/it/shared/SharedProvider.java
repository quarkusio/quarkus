package io.quarkus.it.shared;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

@Produces({ "application/json", "text/plain" })
@Consumes({ "application/json", "text/plain" })
public class SharedProvider implements MessageBodyReader<Shared>, MessageBodyWriter<Shared> {
    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return Shared.class.equals(type);
    }

    @Override
    public Shared readFrom(Class<Shared> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {
        return new Shared("canned");
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return Shared.class.equals(type);
    }

    @Override
    public void writeTo(Shared shared, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException, WebApplicationException {
        entityStream.write(String.format("{\"message\": \"canned+%s\"}", shared.getMessage()).getBytes(StandardCharsets.UTF_8));
    }
}
