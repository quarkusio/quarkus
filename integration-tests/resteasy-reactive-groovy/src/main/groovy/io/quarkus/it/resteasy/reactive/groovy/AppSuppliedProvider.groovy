package io.quarkus.it.resteasy.reactive.groovy

import groovy.transform.CompileStatic
import io.quarkus.it.shared.Shared
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.Produces
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.MultivaluedMap
import jakarta.ws.rs.ext.MessageBodyReader
import jakarta.ws.rs.ext.MessageBodyWriter
import jakarta.ws.rs.ext.Provider

import java.lang.annotation.Annotation
import java.lang.reflect.Type
import java.nio.charset.StandardCharsets

@CompileStatic
@Provider
@Produces("text/plain")
@Consumes("text/plain")
class AppSuppliedProvider implements MessageBodyReader<Shared>, MessageBodyWriter<Shared> {

    @Override
    boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        type == Shared.class
    }

    @Override
    Shared readFrom(Class<Shared> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        new Shared("app")
    }

    @Override
    boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        type == Shared.class
    }

    @Override
    void writeTo(Shared shared, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        entityStream?.write(
                sprintf("{\"message\": \"app+%s\"}", shared?.message)
                .getBytes(StandardCharsets.UTF_8)
        )
    }
}
