package io.quarkus.rest.client.reactive.jackson.runtime.serialisers;

import static io.quarkus.resteasy.reactive.jackson.runtime.serialisers.JacksonMessageBodyWriterUtil.createDefaultWriter;
import static io.quarkus.resteasy.reactive.jackson.runtime.serialisers.JacksonMessageBodyWriterUtil.doLegacyWrite;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public class ClientJacksonMessageBodyWriter implements MessageBodyWriter<Object> {

    protected final ObjectMapper originalMapper;
    protected final ObjectWriter defaultWriter;

    @Inject
    public ClientJacksonMessageBodyWriter(ObjectMapper mapper) {
        this.originalMapper = mapper;
        this.defaultWriter = createDefaultWriter(mapper);
    }

    @Override
    public boolean isWriteable(Class type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    @Override
    public void writeTo(Object o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        doLegacyWrite(o, annotations, httpHeaders, entityStream, defaultWriter);
    }
}
