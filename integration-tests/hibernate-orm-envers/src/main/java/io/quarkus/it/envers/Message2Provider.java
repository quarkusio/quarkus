package io.quarkus.it.envers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;

@Provider
@Consumes(MediaType.WILDCARD)
@Produces(MediaType.WILDCARD)
public class Message2Provider implements MessageBodyReader<Message2>, MessageBodyWriter<Message2> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return Message2.class.isAssignableFrom(type);
    }

    @Override
    public Message2 readFrom(Class<Message2> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        return new Message2("in");
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return Message2.class.isAssignableFrom(type);
    }

    @Override
    public void writeTo(Message2 event, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        String data = "out";
        if (annotations != null) {
            for (Annotation annotation : annotations) {
                if (annotation.annotationType().equals(CustomOutput.class)) {
                    data = ((CustomOutput) annotation).value();
                    break;
                }
            }
        }
        entityStream.write(String.format("{\"data\": \"%s\"}", data).getBytes(StandardCharsets.UTF_8));
    }
}
