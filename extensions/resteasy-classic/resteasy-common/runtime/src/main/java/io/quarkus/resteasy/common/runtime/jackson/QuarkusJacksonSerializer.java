package io.quarkus.resteasy.common.runtime.jackson;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import javax.activation.DataSource;
import javax.annotation.Priority;
import javax.ws.rs.Consumes;
import javax.ws.rs.Priorities;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.plugins.providers.FileRange;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider;
import org.jboss.resteasy.spi.AsyncOutputStream;
import org.jboss.resteasy.spi.AsyncStreamingOutput;

/**
 * provider that can produce JSON by default, removing the need for @Produces and @Consumes everywhere
 */
@Provider
@Produces(MediaType.WILDCARD)
@Consumes(MediaType.WILDCARD)
@Priority(Priorities.USER - 200)
public class QuarkusJacksonSerializer extends ResteasyJackson2Provider {

    /**
     * RESTEasy can already handle these
     */
    private static final Set<Class<?>> BUILTIN_DEFAULTS = new HashSet<>(
            Arrays.asList(String.class, InputStream.class, FileRange.class, AsyncStreamingOutput.class, DataSource.class,
                    Reader.class, StreamingOutput.class, byte[].class, File.class));

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        if (BUILTIN_DEFAULTS.contains(type)) {
            return false;
        }
        return super.isReadable(type, genericType, annotations, mediaType);
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        if (BUILTIN_DEFAULTS.contains(type)) {
            return false;
        }
        return mediaType.equals(MediaType.APPLICATION_OCTET_STREAM_TYPE)
                || mediaType.isWildcardType()
                || super.isWriteable(type, genericType, annotations, mediaType);
    }

    @Override
    public void writeTo(Object t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        httpHeaders.putSingle(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        super.writeTo(t, type, genericType, annotations, mediaType, httpHeaders, entityStream);
    }

    @Override
    public CompletionStage<Void> asyncWriteTo(Object t, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, AsyncOutputStream entityStream) {
        httpHeaders.putSingle(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        return super.asyncWriteTo(t, type, genericType, annotations, mediaType, httpHeaders, entityStream);
    }
}
