package io.quarkus.resteasy.common.runtime.jsonb;

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

import jakarta.activation.DataSource;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.StreamingOutput;
import jakarta.ws.rs.ext.Provider;

import org.jboss.resteasy.plugins.providers.FileRange;
import org.jboss.resteasy.plugins.providers.jsonb.JsonBindingProvider;
import org.jboss.resteasy.spi.AsyncOutputStream;
import org.jboss.resteasy.spi.AsyncStreamingOutput;

/**
 * provider that can produce JSON by default, removing the need for @Produces and @Consumes everywhere
 */
@Provider
@Produces(MediaType.WILDCARD)
@Consumes(MediaType.WILDCARD)
@Priority(Priorities.USER - 200)
public class QuarkusJsonbSerializer extends JsonBindingProvider {

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
        return isSupportedMediaType(mediaType);
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        if (BUILTIN_DEFAULTS.contains(type)) {
            return false;
        }
        return isSupportedMediaType(mediaType) || mediaType.equals(MediaType.APPLICATION_OCTET_STREAM_TYPE)
                || mediaType.isWildcardType();
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
