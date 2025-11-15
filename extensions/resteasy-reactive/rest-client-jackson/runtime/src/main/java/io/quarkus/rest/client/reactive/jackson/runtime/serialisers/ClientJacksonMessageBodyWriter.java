package io.quarkus.rest.client.reactive.jackson.runtime.serialisers;

import static io.quarkus.rest.client.reactive.jackson.runtime.serialisers.JacksonUtil.getObjectMapperFromContext;
import static io.quarkus.rest.client.reactive.jackson.runtime.serialisers.JacksonUtil.matchingView;
import static org.jboss.resteasy.reactive.server.jackson.JacksonMessageBodyWriterUtil.createDefaultWriter;
import static org.jboss.resteasy.reactive.server.jackson.JacksonMessageBodyWriterUtil.doLegacyWrite;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;

import org.jboss.resteasy.reactive.client.impl.RestClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ClientMessageBodyWriter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public class ClientJacksonMessageBodyWriter implements ClientMessageBodyWriter<Object> {

    private final ObjectWriter defaultWriter;
    private final ConcurrentMap<ObjectMapper, ObjectWriter> objectWriterMap = new ConcurrentHashMap<>();

    @Inject
    public ClientJacksonMessageBodyWriter(ObjectMapper mapper) {
        this.defaultWriter = createDefaultWriter(mapper);
    }

    @Override
    public boolean isWriteable(Class type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    @Override
    public void writeTo(Object o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        doLegacyWrite(o, annotations, httpHeaders, entityStream, getEffectiveWriter(mediaType, annotations, null));
    }

    @Override
    public void writeTo(Object o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream,
            RestClientRequestContext context) throws IOException, WebApplicationException {
        doLegacyWrite(o, annotations, httpHeaders, entityStream, getEffectiveWriter(mediaType, annotations, context));
    }

    protected ObjectWriter getEffectiveWriter(MediaType responseMediaType, Annotation[] annotations,
            RestClientRequestContext context) {
        ObjectWriter result = defaultWriter;
        ObjectMapper objectMapper = getObjectMapperFromContext(responseMediaType, context);
        if (objectMapper != null) {
            result = objectWriterMap.computeIfAbsent(objectMapper, new Function<>() {
                @Override
                public ObjectWriter apply(ObjectMapper objectMapper) {
                    return createDefaultWriter(objectMapper);
                }
            });
        }
        return applyJsonViewIfPresent(result, effectiveView(annotations, context));
    }

    private Optional<Class<?>> effectiveView(Annotation[] annotations, RestClientRequestContext context) {
        Optional<Class<?>> fromAnnotations = matchingView(annotations);
        if (fromAnnotations.isPresent()) {
            return fromAnnotations;
        }

        // now check the method parameters for a @JsonView on the body parameter
        if (context != null && context.getInvokedMethod() != null) {
            Parameter[] parameters = context.getInvokedMethod().getParameters();
            if (parameters != null) {
                for (Parameter parameter : parameters) {
                    Annotation[] paramAnnotations = parameter.getAnnotations();
                    boolean isBodyParameter = true;
                    for (Annotation paramAnnotation : paramAnnotations) {
                        String paramTypeClassName = paramAnnotation.annotationType().getName();
                        // TODO: this should be centralized somewhere
                        if (paramTypeClassName.startsWith("jakarta.ws.rs")
                                || paramTypeClassName.startsWith("io.quarkus.rest.client")
                                || paramTypeClassName.startsWith("org.jboss.resteasy.reactive")) {
                            isBodyParameter = false;
                            break;
                        }
                    }
                    if (isBodyParameter) {
                        return matchingView(paramAnnotations);
                    }
                }
            }
        }

        return Optional.empty();
    }

    private static ObjectWriter applyJsonViewIfPresent(ObjectWriter writer, Optional<Class<?>> maybeView) {
        if (maybeView.isPresent()) {
            return writer.withView(maybeView.get());
        } else {
            return writer;
        }
    }
}
