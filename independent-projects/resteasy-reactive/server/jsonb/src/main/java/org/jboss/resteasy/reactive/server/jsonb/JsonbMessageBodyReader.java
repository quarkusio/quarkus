package org.jboss.resteasy.reactive.server.jsonb;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.common.providers.serialisers.AbstractJsonMessageBodyReader;
import org.jboss.resteasy.reactive.common.util.StreamUtil;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyReader;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

public class JsonbMessageBodyReader extends AbstractJsonMessageBodyReader implements ServerMessageBodyReader<Object> {

    private final Jsonb json;

    @Inject
    public JsonbMessageBodyReader(Jsonb json) {
        this.json = json;
    }

    @Override
    public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        return doReadFrom(type, genericType, entityStream);
    }

    @Override
    public Object readFrom(Class<Object> type, Type genericType, MediaType mediaType, ServerRequestContext context)
            throws WebApplicationException, IOException {
        return doReadFrom(type, genericType, context.getInputStream());
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return isReadable(mediaType, type);
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, ResteasyReactiveResourceInfo lazyMethod, MediaType mediaType) {
        return isReadable(mediaType, type);
    }

    private Object doReadFrom(Class<Object> type, Type genericType, InputStream entityStream) {
        if (StreamUtil.isEmpty(entityStream)) {
            return null;
        }
        try {
            return json.fromJson(entityStream, genericType != null ? genericType : type);
        } catch (JsonbException e) {
            if (isEmptyInputException(e)) {
                return null;
            }
            if (isInputTooDeeplyNestedException(e)) {
                // excessively nested JSON is a client error, other JsonbExceptions
                // (e.g. type definition problems) are not
                throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
            }
            throw e;
        } catch (StackOverflowError e) {
            // Yasson deserializes nested JSON recursively and can exhaust the thread stack before the
            // parser's nesting limit is reached. Treat such excessively nested input as a client error
            // instead of letting it escape as an unmapped 500 (see GHSA-mf6v-cw62-468m). The cause is
            // intentionally not attached to avoid carrying around (and potentially logging) its huge stack trace.
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
    }

    private boolean isInputTooDeeplyNestedException(Exception e) {
        // Parsson rejects input nested beyond its limit with a plain RuntimeException ("Input is too deeply
        // nested"), which Yasson wraps in a JsonbException; it is not a JsonParsingException. Yasson may also
        // wrap a StackOverflowError from its own recursive deserialization. Both are client errors.
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof StackOverflowError) {
                return true;
            }
            String message = t.getMessage();
            if (message != null && message.contains("too deeply nested")) {
                return true;
            }
        }
        return false;
    }

    private boolean isEmptyInputException(Exception e) {
        // this isn't great, but Yasson doesn't have a specific exception for empty input...
        return e.getMessage().contains("Invalid token=EOF at (line no=1, column no=0");
    }
}
