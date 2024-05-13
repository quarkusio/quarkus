package io.quarkus.rest.client.reactive;

import static org.jboss.resteasy.reactive.server.jackson.JacksonMessageBodyWriterUtil.doLegacyWrite;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

@Produces("application/json")
public class TestJacksonBasicMessageBodyWriter implements MessageBodyWriter<Object> {

    private final ObjectWriter wWriter;

    public TestJacksonBasicMessageBodyWriter() {
        this.wWriter = new ObjectMapper().writer();
    }

    @Override
    public final boolean isWriteable(Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    @Override
    public void writeTo(Object o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException,
            WebApplicationException {
        doLegacyWrite(o, annotations, httpHeaders, entityStream, wWriter);
    }

}
